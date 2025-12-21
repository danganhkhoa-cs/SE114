/**
 * Import SDKs
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

// 1. Định nghĩa Secrets
const gmailUser = defineSecret("GMAIL_USER");
const gmailPass = defineSecret("GMAIL_PASS");

// Cấu hình mailer
const createTransporter = (user, pass) => {
	return nodemailer.createTransport({
		service: "gmail",
		auth: {
			user: user,
			pass: pass,
		},
	});
};

/**
 * FUNCTION 1: Gửi OTP (Send OTP)
 */
exports.sendOtp = onCall(
	{
		region: "asia-southeast1",
		secrets: [gmailUser, gmailPass],
	},
	async (request) => {
		const email = request.data.email;

		if (!email) {
			throw new HttpsError("invalid-argument", "Vui lòng nhập Email.");
		}

		try {
			await admin.auth().getUserByEmail(email);
		} catch (error) {
			throw new HttpsError(
				"not-found",
				"Email này chưa được đăng ký tài khoản."
			);
		}

		const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
		const expiresAt = Date.now() + 5 * 60 * 1000;

		await admin.firestore().collection("password_resets").doc(email).set({
			otp: otpCode,
			expiresAt: expiresAt,
			createdAt: admin.firestore.FieldValue.serverTimestamp(),
		});

		const transporter = createTransporter(gmailUser.value(), gmailPass.value());

		const mailOptions = {
			from: "LocaSOS Support <no-reply@locasos.com>",
			to: email,
			subject: "LocaSOS - Mã xác thực đặt lại mật khẩu",
			html: `
                <div style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2>Xin chào,</h2>
                    <p>Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản LocaSOS.</p>
                    <p>Mã OTP của bạn là:</p>
                    <h1 style="color: #00796B; letter-spacing: 5px;">${otpCode}</h1>
                    <p>Mã này sẽ hết hạn sau 5 phút.</p>
                    <p>Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>
                </div>
            `,
		};

		try {
			await transporter.sendMail(mailOptions);
			return { success: true, message: "Đã gửi mã OTP thành công." };
		} catch (error) {
			console.error("Lỗi gửi mail:", error);
			throw new HttpsError("internal", "Lỗi server: Không thể gửi email.");
		}
	}
);

/**
 * FUNCTION 2: Đặt lại mật khẩu (Reset Password)
 */
exports.resetPassword = onCall(
	{
		region: "asia-southeast1",
	},
	async (request) => {
		const { email, otp, newPassword } = request.data;

		if (!email || !otp || !newPassword) {
			throw new HttpsError("invalid-argument", "Thiếu thông tin xác thực.");
		}

		if (newPassword.length < 6) {
			throw new HttpsError(
				"invalid-argument",
				"Mật khẩu phải từ 6 ký tự trở lên."
			);
		}
		const docRef = admin.firestore().collection("password_resets").doc(email);
		const docSnap = await docRef.get();

		if (!docSnap.exists) {
			throw new HttpsError(
				"not-found",
				"Yêu cầu hết hạn hoặc không tồn tại. Vui lòng gửi lại mã."
			);
		}

		const data = docSnap.data();

		if (data.otp !== otp) {
			throw new HttpsError("permission-denied", "Mã OTP không chính xác.");
		}

		if (Date.now() > data.expiresAt) {
			throw new HttpsError("deadline-exceeded", "Mã OTP đã hết hạn.");
		}

		try {
			const userRecord = await admin.auth().getUserByEmail(email);

			await admin.auth().updateUser(userRecord.uid, {
				password: newPassword,
			});

			await docRef.delete();

			return { success: true, message: "Đổi mật khẩu thành công." };
		} catch (error) {
			console.error("Lỗi đổi pass:", error);
			throw new HttpsError("internal", "Lỗi hệ thống khi đổi mật khẩu.");
		}
	}
);

/**
 * FUNCTION 3: Kiểm tra OTP (Verify OTP)
 */
exports.verifyOtp = onCall(
	{
		region: "asia-southeast1",
	},
	async (request) => {
		const { email, otp } = request.data;

		if (!email || !otp) {
			throw new HttpsError("invalid-argument", "Thiếu thông tin.");
		}

		// 1. Lấy dữ liệu
		const docRef = admin.firestore().collection("password_resets").doc(email);
		const docSnap = await docRef.get();

		if (!docSnap.exists) {
			throw new HttpsError(
				"not-found",
				"Mã OTP không tồn tại hoặc đã hết hạn."
			);
		}

		const data = docSnap.data();

		// 2. So sánh OTP
		if (data.otp !== otp) {
			throw new HttpsError("permission-denied", "Mã OTP không chính xác.");
		}

		// 3. Kiểm tra hạn sử dụng
		if (Date.now() > data.expiresAt) {
			throw new HttpsError("deadline-exceeded", "Mã OTP đã hết hạn.");
		}

		// Nếu mọi thứ ok -> Trả về thành công
		return { success: true, message: "OTP hợp lệ." };
	}
);
