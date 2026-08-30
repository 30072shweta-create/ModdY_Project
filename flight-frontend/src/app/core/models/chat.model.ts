export type ChatDomain =
  | 'FLIGHTS_AND_SEATS'
  | 'BOOKING'
  | 'PAYMENT'
  | 'CANCELLATION_AND_REFUND'
  | 'PRICING_AND_COUPONS';

export interface DomainResponse {
  name: string;
  value: ChatDomain;
}

export interface ChatRequest {
  conversationId: string;
  domain: ChatDomain;
  message: string;
}

export interface ChatResponse {
  conversationId: string;
  domain: ChatDomain;
  answer: string;
}
