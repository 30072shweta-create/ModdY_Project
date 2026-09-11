import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FlightsService } from '../../../core/services/flights.service';
import { AircraftService } from '../../../core/services/aircraft.service';
import { WeatherService } from '../../../core/services/weather.service';
import { FlightResponseDTO, AircraftResponseDTO } from '../../../core/models/flight.model';
import { RouteWeather } from '../../../core/models/weather.model';

@Component({
  selector: 'app-flight-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './flight-detail.component.html',
  styleUrls: ['./flight-detail.component.scss']
})
export class FlightDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private flightsService = inject(FlightsService);
  private aircraftService = inject(AircraftService);
  private weatherService = inject(WeatherService);

  public flight: FlightResponseDTO | null = null;
  public aircraft: AircraftResponseDTO | null = null;
  public routeWeather: RouteWeather | null = null;
  public isLoading = true;
  public errorMessage = '';

  public ngOnInit(): void {
    const flightId = Number(this.route.snapshot.paramMap.get('id'));
    if (flightId) {
      this.loadFlight(flightId);
    }
  }

  private loadFlight(flightId: number): void {
    this.isLoading = true;
    this.flightsService.getFlightById(flightId).subscribe({
      next: (data) => {
        this.flight = data;
        this.isLoading = false;
        if (data.aircraftId) {
          this.loadAircraft(data.aircraftId);
        }
        if (data.fromAirport && data.toAirport) {
          this.weatherService.getRouteWeather(data.fromAirport, data.toAirport).subscribe({
            next: (rw) => this.routeWeather = rw,
            error: () => {}
          });
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Flight details not found.';
      }
    });
  }

  private loadAircraft(aircraftId: number): void {
    this.aircraftService.getAircraftById(aircraftId).subscribe({
      next: (data) => this.aircraft = data
    });
  }

  public proceedToBooking(): void {
    if (this.flight) {
      this.router.navigate(['/booking'], { queryParams: { flightId: this.flight.flightId } });
    }
  }
}
