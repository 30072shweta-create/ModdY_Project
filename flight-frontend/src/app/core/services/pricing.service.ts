import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FareCalculationRequestDTO,
  FareBreakdownDTO,
  FlightPricingRequestDTO,
  FlightPricingResponseDTO,
  PricingRuleRequestDTO,
  PricingRuleResponseDTO
} from '../models/pricing.model';

@Injectable({
  providedIn: 'root'
})
export class PricingService {
  private apiUrl = `${environment.apiUrl}/api/flight-pricing`;
  private rulesUrl = `${environment.apiUrl}/api/pricing-rules`;

  constructor(private http: HttpClient) {}

  public calculateFare(dto: FareCalculationRequestDTO): Observable<FareBreakdownDTO> {
    return this.http.post<FareBreakdownDTO>(`${this.apiUrl}/calculate`, dto);
  }

  public getAllPricing(): Observable<FlightPricingResponseDTO[]> {
    return this.http.get<FlightPricingResponseDTO[]>(this.apiUrl);
  }

  public getPricingByFlight(flightId: number): Observable<FlightPricingResponseDTO[]> {
    return this.http.get<FlightPricingResponseDTO[]>(`${this.apiUrl}/flight/${flightId}`);
  }

  public addPricing(dto: FlightPricingRequestDTO): Observable<FlightPricingResponseDTO> {
    return this.http.post<FlightPricingResponseDTO>(this.apiUrl, dto);
  }

  public updatePricing(pricingId: number, dto: FlightPricingRequestDTO): Observable<FlightPricingResponseDTO> {
    return this.http.put<FlightPricingResponseDTO>(`${this.apiUrl}/${pricingId}`, dto);
  }

  public deletePricing(pricingId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${pricingId}`);
  }

  // Pricing Rules
  public getAllRules(): Observable<PricingRuleResponseDTO[]> {
    return this.http.get<PricingRuleResponseDTO[]>(this.rulesUrl);
  }

  public createRule(dto: PricingRuleRequestDTO): Observable<PricingRuleResponseDTO> {
    return this.http.post<PricingRuleResponseDTO>(this.rulesUrl, dto);
  }

  public updateRule(ruleId: number, dto: PricingRuleRequestDTO): Observable<PricingRuleResponseDTO> {
    return this.http.put<PricingRuleResponseDTO>(`${this.rulesUrl}/${ruleId}`, dto);
  }

  public deleteRule(ruleId: number): Observable<void> {
    return this.http.delete<void>(`${this.rulesUrl}/${ruleId}`);
  }
}
