export interface UserResponseDTO {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  emailVerified: boolean;
}

export interface AuthResponseDTO {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponseDTO;
}

export interface LoginRequestDTO {
  email: string;
  password: string;
}

export interface RegisterRequestDTO {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface GoogleLoginRequestDTO {
  idToken: string;
}

export interface RefreshTokenRequestDTO {
  refreshToken?: string;
}

export interface VerifyEmailRequestDTO {
  email: string;
  otp: string;
}

export interface ForgotPasswordRequestDTO {
  email: string;
}

export interface ResetPasswordRequestDTO {
  email: string;
  otp: string;
  newPassword: string;
}

export interface UpdateProfileRequestDTO {
  firstName: string;
  lastName: string;
}

export interface ChangePasswordRequestDTO {
  oldPassword: string;
  newPassword: string;
}
