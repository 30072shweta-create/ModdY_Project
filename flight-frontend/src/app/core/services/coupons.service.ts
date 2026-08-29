import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CouponRequestDTO, CouponResponseDTO } from '../models/pricing.model';

@Injectable({
  providedIn: 'root'
})
export class CouponsService {
  private apiUrl = `${environment.apiUrl}/api/coupons`;

  constructor(private http: HttpClient) {}

  public validateCoupon(couponCode: string, amount?: number): Observable<CouponResponseDTO> {
    const params: any = {};
    if (amount !== undefined && amount !== null) {
      params.amount = amount.toString();
    }
    return this.http.get<CouponResponseDTO>(`${this.apiUrl}/validate/${encodeURIComponent(couponCode)}`, { params });
  }

  public getAllCoupons(): Observable<CouponResponseDTO[]> {
    return this.http.get<CouponResponseDTO[]>(this.apiUrl);
  }

  public createCoupon(dto: CouponRequestDTO): Observable<CouponResponseDTO> {
    return this.http.post<CouponResponseDTO>(this.apiUrl, dto);
  }

  public updateCoupon(couponId: number, dto: CouponRequestDTO): Observable<CouponResponseDTO> {
    return this.http.put<CouponResponseDTO>(`${this.apiUrl}/${couponId}`, dto);
  }

  public deleteCoupon(couponId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${couponId}`);
  }
}
