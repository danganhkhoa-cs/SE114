/**
 * Import SDKs
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
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

		const docRef = admin.firestore().collection("password_resets").doc(email);
		const docSnap = await docRef.get();

		if (!docSnap.exists) {
			throw new HttpsError(
				"not-found",
				"Mã OTP không tồn tại hoặc đã hết hạn."
			);
		}

		const data = docSnap.data();

		if (data.otp !== otp) {
			throw new HttpsError("permission-denied", "Mã OTP không chính xác.");
		}

		if (Date.now() > data.expiresAt) {
			throw new HttpsError("deadline-exceeded", "Mã OTP đã hết hạn.");
		}

		return { success: true, message: "OTP hợp lệ." };
	}
);

/**
 * FUNCTION 4: Gửi thông báo (Sửa lại cấu hình trigger)
 */
exports.sendNotification = onDocumentCreated(
	{
		document: "users/{userId}/notifications/{notificationId}",
		region: "asia-southeast1",
	},
	async (event) => {
		// ... Code xử lý giữ nguyên ...

		const userId = event.params.userId;
		const snapshot = event.data;

		if (!snapshot) {
			console.log("No data associated with the event");
			return;
		}

		const notificationData = snapshot.data();

		// 1. Lấy thông tin người nhận để tìm FCM Token
		const userDoc = await admin
			.firestore()
			.collection("users")
			.doc(userId)
			.get();

		if (!userDoc.exists) return null;

		const fcmToken = userDoc.data().fcm_token;
		if (!fcmToken) {
			console.log("No FCM token for user: ", userId);
			return null;
		}

		// 2. Chuẩn bị nội dung
		const title = "LocaSOS";
		// Thêm check để tránh crash nếu message bị null
		const body = `${notificationData.senderName || "Someone"} ${
			notificationData.message || "sent a notification"
		}`;

		// 3. Tạo Message
		const message = {
			token: fcmToken,
			notification: {
				title: title,
				body: body,
			},
			data: {
				postId: notificationData.postId || "",
				click_action: "FLUTTER_NOTIFICATION_CLICK",
			},
			android: {
				priority: "high",
				notification: {
					sound: "default",
					channelId: "locasos_channel_id",
				},
			},
		};

		// 4. Gửi thông báo
		try {
			await admin.messaging().send(message);
			console.log("Notification sent successfully to: ", userId);
		} catch (error) {
			console.error("Error sending notification: ", error);
		}
		return null;
	}
);

/**
 * FUNCTION 5: Gửi thông báo hệ thống (Broadcast qua Topic)
 * Trigger: Khi có document mới trong collection "system_notifications"
 */
exports.sendSystemNotification = onDocumentCreated(
	{
		document: "system_notifications/{notificationId}",
		region: "asia-southeast1",
	},
	async (event) => {
		const snapshot = event.data;
		if (!snapshot) return;

		const data = snapshot.data();
		const title = data.title || "LocaSOS System";
		const body = data.message || "New announcement";

		// Tạo Message gửi cho Topic "global_alerts"
		const message = {
			topic: "global_alerts", // Gửi cho tất cả ai đăng ký topic này
			notification: {
				title: title,
				body: body,
			},
			data: {
				click_action: "FLUTTER_NOTIFICATION_CLICK",
				type: "SYSTEM", // Để App nhận biết đây là tin hệ thống
				notificationId: event.params.notificationId,
			},
			android: {
				priority: "high",
				notification: {
					channelId: "locasos_channel_id",
				},
			},
		};

		try {
			const response = await admin.messaging().send(message);
			console.log("Successfully sent system message:", response);
		} catch (error) {
			console.error("Error sending system message:", error);
		}
	}
);
