import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { FlightsService } from '../../../core/services/flights.service';
import { AirportsService } from '../../../core/services/airports.service';
import { AirlinesService } from '../../../core/services/airlines.service';
import { WeatherService } from '../../../core/services/weather.service';
import { FlightResponseDTO, FlightSearchRequestDTO, AirportResponseDTO, AirlineResponseDTO, Page } from '../../../core/models/flight.model';
import { RouteWeather } from '../../../core/models/weather.model';

@Component({
  selector: 'app-flight-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './flight-list.component.html',
  styleUrls: ['./flight-list.component.scss']
})
export class FlightListComponent implements OnInit {
  private fb = inject(FormBuilder);
  private flightsService = inject(FlightsService);
  private airportsService = inject(AirportsService);
  private airlinesService = inject(AirlinesService);
  private weatherService = inject(WeatherService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  public flightsPage: Page<FlightResponseDTO> | null = null;
  public airports: AirportResponseDTO[] = [];
  public airlines: AirlineResponseDTO[] = [];
  public routeWeather: RouteWeather | null = null;
  public isLoadingWeather = false;

  public isLoading = true;
  public isSearching = false;
  public errorMessage = '';
  public dateError = '';

  public filterForm: FormGroup = this.fb.group({
    tripType: ['roundTrip'],
    source: [''],
    destination: [''],
    date: [''],
    returnDate: [''],
    airline: [''],
    cabinClass: ['ECONOMY'],
    passengers: [1],
    stops: [''],
    maxPrice: [50000],
    sortBy: ['basePrice'],
    sortDirection: ['ASC'],
    page: [0],
    size: [10]
  });

  public ngOnInit(): void {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const depDateStr = tomorrow.toISOString().split('T')[0];

    const returnDate = new Date(tomorrow);
    returnDate.setDate(returnDate.getDate() + 3);
    const retDateStr = returnDate.toISOString().split('T')[0];

    this.loadAirports();
    this.loadAirlines();

    this.route.queryParams.subscribe(params => {
      this.filterForm.patchValue({
        source: params['source'] || '',
        destination: params['destination'] || '',
        date: params['date'] || depDateStr,
        returnDate: params['returnDate'] || retDateStr,
        tripType: params['tripType'] || (params['returnDate'] ? 'roundTrip' : 'oneWay'),
        airline: params['airline'] || '',
        cabinClass: params['cabinClass'] || 'ECONOMY',
        passengers: params['passengers'] ? Number(params['passengers']) : 1,
        stops: params['stops'] !== undefined ? params['stops'] : '',
        sortBy: params['sortBy'] || 'basePrice',
        sortDirection: params['sortDirection'] || 'ASC'
      }, { emitEvent: false });

      this.executeSearch();
    });

    this.filterForm.get('date')?.valueChanges.subscribe(() => this.validateDates());
    this.filterForm.get('returnDate')?.valueChanges.subscribe(() => this.validateDates());
  }

  private loadAirports(): void {
    this.airportsService.getAllAirports().subscribe({
      next: (data) => this.airports = data || [],
      error: () => this.airports = []
    });
  }

  private loadAirlines(): void {
    this.airlinesService.getAllAirlines().subscribe({
      next: (data) => this.airlines = data || [],
      error: () => this.airlines = []
    });
  }

  public setTripType(type: 'roundTrip' | 'oneWay'): void {
    this.filterForm.patchValue({ tripType: type });
    this.validateDates();
  }

  public get isRoundTrip(): boolean {
    return this.filterForm.get('tripType')?.value === 'roundTrip';
  }

  public swapAirports(): void {
    const src = this.filterForm.get('source')?.value;
    const dest = this.filterForm.get('destination')?.value;
    this.filterForm.patchValue({
      source: dest,
      destination: src
    });
  }

  public incrementPassengers(): void {
    const current = Number(this.filterForm.get('passengers')?.value || 1);
    if (current < 9) {
      this.filterForm.patchValue({ passengers: current + 1 });
    }
  }

  public decrementPassengers(): void {
    const current = Number(this.filterForm.get('passengers')?.value || 1);
    if (current > 1) {
      this.filterForm.patchValue({ passengers: current - 1 });
    }
  }

  public validateDates(): boolean {
    this.dateError = '';
    const dep = this.filterForm.get('date')?.value;
    const ret = this.filterForm.get('returnDate')?.value;

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

  public onSearchSubmit(): void {
    if (!this.validateDates()) {
      return;
    }
    this.filterForm.patchValue({ page: 0 });
    const val = this.filterForm.value;
    const queryParams: any = {
      source: val.source || null,
      destination: val.destination || null,
      date: val.date || null,
      airline: val.airline || null,
      cabinClass: val.cabinClass || null,
      passengers: val.passengers || null,
      tripType: val.tripType || null,
      stops: val.stops !== '' ? val.stops : null,
      sortBy: val.sortBy,
      sortDirection: val.sortDirection,
      page: 0
    };
    if (val.tripType === 'roundTrip' && val.returnDate) {
      queryParams.returnDate = val.returnDate;
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge'
    });
    this.executeSearch();
  }

  public executeSearch(): void {
    this.isLoading = true;
    this.isSearching = true;
    this.errorMessage = '';

    const from = this.filterForm.value.source;
    const to = this.filterForm.value.destination;

    if (from && to) {
      this.isLoadingWeather = true;
      this.weatherService.getRouteWeather(from, to).subscribe({
        next: (rw) => {
          this.routeWeather = rw;
          this.isLoadingWeather = false;
        },
        error: () => {
          this.isLoadingWeather = false;
        }
      });
    } else {
      this.routeWeather = null;
    }

    const rawStops = this.filterForm.value.stops;
    const { tripType, returnDate, cabinClass, passengers, ...formValues } = this.filterForm.value;
    const query: FlightSearchRequestDTO = {
      ...formValues
    };

    if (rawStops === '' || rawStops === null || rawStops === undefined) {
      delete query.stops;
    } else {
      query.stops = Number(rawStops);
    }
    if (query.airline === '') delete query.airline;
    if (!query.source) delete query.source;
    if (!query.destination) delete query.destination;
    if (!query.date) delete query.date;

    this.flightsService.searchFlights(query).subscribe({
      next: (page) => {
        this.flightsPage = page;
        this.isLoading = false;
        this.isSearching = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.isSearching = false;
        this.errorMessage = err.error?.message || err.error || 'Failed to load flight results. Please try again.';
      }
    });
  }

  public applyFilters(): void {
    this.filterForm.patchValue({ page: 0 });
    this.executeSearch();
  }

  public onSortChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    const [sortBy, sortDirection] = val.split('_');
    this.filterForm.patchValue({ sortBy, sortDirection, page: 0 });
    this.executeSearch();
  }

  public changePage(newPage: number): void {
    if (newPage >= 0 && this.flightsPage && newPage < this.flightsPage.totalPages) {
      this.filterForm.patchValue({ page: newPage });
      this.executeSearch();
    }
  }

  public selectFlight(flightId: number): void {
    this.router.navigate(['/flight', flightId]);
  }

  public startBooking(flightId: number): void {
    const passengers = this.filterForm.value.passengers || 1;
    const cabinClass = this.filterForm.value.cabinClass || 'ECONOMY';
    this.router.navigate(['/booking'], { queryParams: { flightId, passengers, cabinClass } });
  }
}
