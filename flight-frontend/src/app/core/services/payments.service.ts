import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PaymentRequestDTO,
  PaymentResponseDTO,
  PaymentVerificationDTO,
  RazorpayOptions
} from '../models/payment.model';

declare var Razorpay: any;

@Injectable({
  providedIn: 'root'
})
export class PaymentsService {
  private apiUrl = `${environment.apiUrl}/api/payments`;
  private razorpayScriptLoaded = false;

  constructor(private http: HttpClient) {}

  public loadRazorpayScript(): Promise<boolean> {
    return new Promise(resolve => {
      if (this.razorpayScriptLoaded || typeof Razorpay !== 'undefined') {
        resolve(true);
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => {
        this.razorpayScriptLoaded = true;
        resolve(true);
      };
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
  }

  public createPaymentOrder(dto: PaymentRequestDTO): Observable<PaymentResponseDTO> {
    return this.http.post<PaymentResponseDTO>(`${this.apiUrl}/create-order`, dto);
  }

  public verifyPayment(dto: PaymentVerificationDTO): Observable<PaymentResponseDTO> {
    return this.http.post<PaymentResponseDTO>(`${this.apiUrl}/verify`, dto);
  }

  public retryPayment(paymentId: number): Observable<PaymentResponseDTO> {
    return this.http.post<PaymentResponseDTO>(`${this.apiUrl}/${paymentId}/retry`, {});
  }

  public getPaymentById(paymentId: number): Observable<PaymentResponseDTO> {
    return this.http.get<PaymentResponseDTO>(`${this.apiUrl}/${paymentId}`);
  }

  public getPaymentsByBooking(bookingId: number): Observable<PaymentResponseDTO[]> {
    return me(this.http.get<PaymentResponseDTO[]>(`${this.apiUrl}/booking/${bookingId}`));
  }

  public getMyPayments(): Observable<PaymentResponseDTO[]> {
    return this.http.get<PaymentResponseDTO[]>(`${this.apiUrl}/my-payments`);
  }

  public getAllPayments(): Observable<PaymentResponseDTO[]> {
    return this.http.get<PaymentResponseDTO[]>(this.apiUrl);
  }

  public openRazorpayModal(options: RazorpayOptions): void {
    if (typeof Razorpay !== 'undefined') {
      const rzp = new Razorpay(options);
      rzp.open();
    } else {
      console.error('Razorpay SDK is not loaded');
    }
  }
}

function me<T>(obs: Observable<T>): Observable<T> { return obs; }
