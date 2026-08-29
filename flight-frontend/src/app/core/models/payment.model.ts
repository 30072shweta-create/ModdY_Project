export type PaymentMethod = 'RAZORPAY' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'NET_BANKING' | 'UPI' | 'WALLET';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

export interface PaymentRequestDTO {
  bookingId: number;
  paymentMethod: PaymentMethod;
}

export interface PaymentResponseDTO {
  paymentId: number;
  bookingId: number;
  paymentMethod: PaymentMethod;
  amount: number;
  currency: string;
  status: PaymentStatus;
  razorpayOrderId?: string;
  razorpayPaymentId?: string;
  transactionRef?: string;
  paidAt?: string;
  createdAt?: string;
}

export interface PaymentVerificationDTO {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}

export interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  handler: (response: RazorpayPaymentSuccessHandlerResponse) => void;
  prefill?: {
    name?: string;
    email?: string;
    contact?: string;
  };
  notes?: Record<string, string>;
  theme?: {
    color?: string;
  };
  modal?: {
    ondismiss?: () => void;
  };
}

export interface RazorpayPaymentSuccessHandlerResponse {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}
