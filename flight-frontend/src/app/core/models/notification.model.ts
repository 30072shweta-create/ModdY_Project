export interface NotificationResponseDTO {
  notificationId: number;
  userId: number;
  bookingId?: number;
  message: string;
  recipientEmail?: string;
  type: string;
  channel?: string;
  status: string;
  createdAt?: string;
  sentAt?: string;
  read?: boolean;
}

export interface NotificationRequestDTO {
  userId: number;
  bookingId?: number;
  message: string;
  recipientEmail?: string;
  type: string;
  channel?: string;
}

