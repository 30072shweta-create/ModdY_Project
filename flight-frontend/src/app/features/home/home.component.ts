import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AirportsService } from '../../core/services/airports.service';
import { FlightsService } from '../../core/services/flights.service';
import { AirportResponseDTO, FlightResponseDTO } from '../../core/models/flight.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  private fb = inject(FormBuilder);
  private airportsService = inject(AirportsService);
  private flightsService = inject(FlightsService);
  private router = inject(Router);

  public airports: AirportResponseDTO[] = [];
  public featuredFlights: FlightResponseDTO[] = [];
  public isLoadingAirports = true;
  public isSearching = false;
  public dateError = '';

  public searchForm: FormGroup = this.fb.group({
    tripType: ['roundTrip', [Validators.required]],
    source: ['', [Validators.required]],
    destination: ['', [Validators.required]],
    date: ['', [Validators.required]],
    returnDate: [''],
    cabinClass: ['ECONOMY', [Validators.required]],
    passengers: [1, [Validators.required, Validators.min(1), Validators.max(9)]]
  });

  public ngOnInit(): void {
    // Set default travel dates: tomorrow for departure, +3 days for return
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const depDateStr = tomorrow.toISOString().split('T')[0];

    const returnDate = new Date(tomorrow);
    returnDate.setDate(returnDate.getDate() + 3);
    const retDateStr = returnDate.toISOString().split('T')[0];

    this.searchForm.patchValue({
      date: depDateStr,
      returnDate: retDateStr
    });

    this.loadAirports();
    this.loadFeaturedFlights();

    // Listen to tripType changes to toggle returnDate validators
    this.searchForm.get('tripType')?.valueChanges.subscribe((type) => {
      const returnControl = this.searchForm.get('returnDate');
      if (type === 'roundTrip') {
        returnControl?.setValidators([Validators.required]);
      } else {
        returnControl?.clearValidators();
      }
      returnControl?.updateValueAndValidity();
      this.validateDates();
    });

    // Listen to date changes to validate departure vs return
    this.searchForm.get('date')?.valueChanges.subscribe(() => this.validateDates());
    this.searchForm.get('returnDate')?.valueChanges.subscribe(() => this.validateDates());
  }

  public setTripType(type: 'roundTrip' | 'oneWay'): void {
    this.searchForm.patchValue({ tripType: type });
  }

  public get isRoundTrip(): boolean {
    return this.searchForm.get('tripType')?.value === 'roundTrip';
  }

  private loadAirports(): void {
    this.airportsService.getAllAirports().subscribe({
      next: (data) => {
        this.airports = data || [];
        this.isLoadingAirports = false;
        if (this.airports.length >= 2) {
          const currentSrc = this.searchForm.get('source')?.value;
          const currentDest = this.searchForm.get('destination')?.value;
          if (!currentSrc && !currentDest) {
            this.searchForm.patchValue({
              source: this.airports[0].airportCode,
              destination: this.airports[1].airportCode
            });
          }
        }
      },
      error: () => this.isLoadingAirports = false
    });
  }

  private loadFeaturedFlights(): void {
    this.flightsService.getAllFlights().subscribe({
      next: (flights) => this.featuredFlights = (flights || []).slice(0, 4),
      error: () => {}
    });
  }

  public swapAirports(): void {
    const src = this.searchForm.get('source')?.value;
    const dest = this.searchForm.get('destination')?.value;
    this.searchForm.patchValue({
      source: dest,
      destination: src
    });
  }

  public incrementPassengers(): void {
    const current = Number(this.searchForm.get('passengers')?.value || 1);
    if (current < 9) {
      this.searchForm.patchValue({ passengers: current + 1 });
    }
  }

  public decrementPassengers(): void {
    const current = Number(this.searchForm.get('passengers')?.value || 1);
    if (current > 1) {
      this.searchForm.patchValue({ passengers: current - 1 });
    }
  }

  public validateDates(): boolean {
    this.dateError = '';
    const dep = this.searchForm.get('date')?.value;
    const ret = this.searchForm.get('returnDate')?.value;

    if (dep && ret && this.isRoundTrip) {
      if (new Date(ret) < new Date(dep)) {
        this.dateError = 'Return date cannot be earlier than departure date.';
        return false;
      }
    }
    return true;
  }

  public getSelectedAirportDetails(code: string): { city: string; code: string; name: string } {
    const ap = this.airports.find((a) => a.airportCode === code);
    if (ap) {
      return {
        city: ap.city || 'Airport',
        code: ap.airportCode,
        name: ap.name || ap.airportName || `${ap.city} Airport`
      };
    }
    return { city: code || 'Select', code: code || '', name: 'Airport Selection' };
  }

  public onSearch(): void {
    if (!this.validateDates()) {
      return;
    }

    if (this.searchForm.invalid) {
      this.searchForm.markAllAsTouched();
      return;
    }

    const val = this.searchForm.value;
    const queryParams: any = {
      source: val.source,
      destination: val.destination,
      date: val.date,
      cabinClass: val.cabinClass,
      passengers: val.passengers,
      tripType: val.tripType
    };

    if (val.tripType === 'roundTrip' && val.returnDate) {
      queryParams.returnDate = val.returnDate;
    }

    this.isSearching = true;
    this.router.navigate(['/flights'], { queryParams }).then(() => {
      this.isSearching = false;
    });
  }

  public selectFlight(flightId: number): void {
    this.router.navigate(['/flight', flightId]);
  }
}
