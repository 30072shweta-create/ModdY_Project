import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { FlightsService } from '../../../core/services/flights.service';
import { AirportsService } from '../../../core/services/airports.service';
import { AirlinesService } from '../../../core/services/airlines.service';
import { WeatherService } from '../../../core/services/weather.service';
import { FlightResponseDTO, FlightSearchRequestDTO, AirportResponseDTO, AirlineResponseDTO, Page } from '../../../core/models/flight.model';
import { RouteWeather, AirportWeather } from '../../../core/models/weather.model';

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
  public errorMessage = '';

  public filterForm: FormGroup = this.fb.group({
    source: [''],
    destination: [''],
    date: [''],
    airline: [''],
    stops: [''],
    maxPrice: [50000],
    sortBy: ['basePrice'],
    sortDirection: ['ASC'],
    page: [0],
    size: [10]
  });

  public ngOnInit(): void {
    this.loadAirports();
    this.loadAirlines();

    this.route.queryParams.subscribe(params => {
      this.filterForm.patchValue({
        source: params['source'] || '',
        destination: params['destination'] || '',
        date: params['date'] || '',
        airline: params['airline'] || '',
        stops: params['stops'] !== undefined ? params['stops'] : '',
        sortBy: params['sortBy'] || 'basePrice',
        sortDirection: params['sortDirection'] || 'ASC'
      }, { emitEvent: false });

      this.executeSearch();
    });
  }

  private loadAirports(): void {
    this.airportsService.getAllAirports().subscribe({
      next: (data) => this.airports = data
    });
  }

  private loadAirlines(): void {
    this.airlinesService.getAllAirlines().subscribe({
      next: (data) => this.airlines = data
    });
  }

  public executeSearch(): void {
    this.isLoading = true;
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
    const query: FlightSearchRequestDTO = {
      ...this.filterForm.value
    };

    if (rawStops === '' || rawStops === null || rawStops === undefined) {
      delete query.stops;
    } else {
      query.stops = Number(rawStops);
    }
    if (query.airline === '') delete query.airline;

    this.flightsService.searchFlights(query).subscribe({
      next: (page) => {
        this.flightsPage = page;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
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
    this.router.navigate(['/booking'], { queryParams: { flightId } });
  }
}
