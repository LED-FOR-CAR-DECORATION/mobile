package com.carled.app.domain.usecase.iam

/**
 * Tập hợp toàn bộ UseCase của phân hệ IAM, tiện lợi cho việc tiêm phụ thuộc (Dependency Injection).
 */
data class IamUseCases(
    val login: LoginUseCase,
    val register: RegisterUseCase,
    val verifyEmail: VerifyEmailUseCase,
    val resendVerification: ResendVerificationUseCase,
    val googleLogin: GoogleLoginUseCase,
    val refreshToken: RefreshTokenUseCase,
    val logout: LogoutUseCase,
    val forgotPassword: ForgotPasswordUseCase,
    val resetPassword: ResetPasswordUseCase
)
