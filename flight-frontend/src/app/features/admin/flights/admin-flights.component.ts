import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { FlightsService } from '../../../core/services/flights.service';
import { AirportsService } from '../../../core/services/airports.service';
import { AirlinesService } from '../../../core/services/airlines.service';
import { AircraftService } from '../../../core/services/aircraft.service';
import { AuthService } from '../../../core/auth/auth.service';
import {
  FlightResponseDTO,
  AirportResponseDTO,
  AirlineResponseDTO,
  AircraftResponseDTO,
  FlightStatus
} from '../../../core/models/flight.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-flights',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-flights.component.html',
  styleUrls: ['./admin-flights.component.scss']
})
export class AdminFlightsComponent implements OnInit {
  private fb = inject(FormBuilder);
  public authService = inject(AuthService);
  private flightsService = inject(FlightsService);
  private airportsService = inject(AirportsService);
  private airlinesService = inject(AirlinesService);
  private aircraftService = inject(AircraftService);

  public flights: FlightResponseDTO[] = [];
  public airports: AirportResponseDTO[] = [];
  public airlines: AirlineResponseDTO[] = [];
  public aircraftList: AircraftResponseDTO[] = [];

  public flightSearch = '';
  public flightStatusFilter = '';
  public isLoading = false;
  public errorMessage = '';
  public successMessage = '';

  public flightStatuses: FlightStatus[] = ['SCHEDULED', 'BOARDING', 'DEPARTED', 'ARRIVED', 'DELAYED', 'CANCELLED'];

  public showFlightModal = false;
  public editingFlightId: number | null = null;

  public showStatusModal = false;
  public statusUpdateFlight: FlightResponseDTO | null = null;

  public showFlightDetailModal = false;
  public selectedFlightDetail: FlightResponseDTO | null = null;

  public confirmModal = {
    show: false,
    title: '',
    message: '',
    onConfirm: () => { }
  };

  public flightForm: FormGroup = this.fb.group(
    {
      flightNumber: ['', [Validators.required, Validators.maxLength(20)]],
      airlineCode: ['', [Validators.required]],
      fromAirport: ['', [Validators.required]],
      toAirport: ['', [Validators.required]],
      aircraftId: [null, [Validators.required]],
      departureTs: ['', [Validators.required]],
      arrivalTs: ['', [Validators.required]],
      stops: [0, [Validators.required, Validators.min(0)]],
      basePrice: [1000, [Validators.required, Validators.min(0)]],
      availableSeats: [180, [Validators.required, Validators.min(0)]],
      durationMins: [120, [Validators.required, Validators.min(0)]],
      status: ['SCHEDULED', [Validators.required]]
    },
    { validators: [this.routeValidator, this.dateValidator] }
  );

  public statusForm: FormGroup = this.fb.group({
    status: ['SCHEDULED', [Validators.required]]
  });

  public ngOnInit(): void {
    this.loadAllData();
  }

  public loadAllData(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.flightsService.getAllFlights().subscribe({
      next: (data) => {
        this.flights = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });

    this.airportsService.getAllAirports().subscribe({
      next: (data) => (this.airports = data || []),
      error: (err) => console.warn('Failed to load airports', err)
    });

    this.airlinesService.getAllAirlines().subscribe({
      next: (data) => (this.airlines = data || []),
      error: (err) => console.warn('Failed to load airlines', err)
    });

    this.aircraftService.getAllAircraft().subscribe({
      next: (data) => (this.aircraftList = data || []),
      error: (err) => console.warn('Failed to load aircraft', err)
    });
  }

  public get filteredFlights(): FlightResponseDTO[] {
    return this.flights.filter((f) => {
      const matchSearch =
        !this.flightSearch ||
        f.flightNumber.toLowerCase().includes(this.flightSearch.toLowerCase()) ||
        f.airlineCode.toLowerCase().includes(this.flightSearch.toLowerCase()) ||
        f.fromAirport.toLowerCase().includes(this.flightSearch.toLowerCase()) ||
        f.toAirport.toLowerCase().includes(this.flightSearch.toLowerCase());
      const matchStatus = !this.flightStatusFilter || f.status === this.flightStatusFilter;
      return matchSearch && matchStatus;
    });
  }

  public openAddFlightModal(): void {
    this.editingFlightId = null;
    this.flightForm.reset({
      flightNumber: '',
      airlineCode: this.airlines.length > 0 ? this.airlines[0].airlineCode : '',
      fromAirport: this.airports.length > 0 ? this.airports[0].airportCode : '',
      toAirport: this.airports.length > 1 ? this.airports[1].airportCode : (this.airports[0]?.airportCode || ''),
      aircraftId: this.aircraftList.length > 0 ? this.aircraftList[0].aircraftId : null,
      departureTs: '',
      arrivalTs: '',
      stops: 0,
      basePrice: 3000,
      availableSeats: 180,
      durationMins: 120,
      status: 'SCHEDULED'
    });
    this.showFlightModal = true;
  }

  public openEditFlightModal(flight: FlightResponseDTO): void {
    this.editingFlightId = flight.flightId;
    this.flightForm.patchValue({
      flightNumber: flight.flightNumber,
      airlineCode: flight.airlineCode,
      fromAirport: flight.fromAirport,
      toAirport: flight.toAirport,
      aircraftId: flight.aircraftId,
      departureTs: flight.departureTs ? flight.departureTs.substring(0, 16) : '',
      arrivalTs: flight.arrivalTs ? flight.arrivalTs.substring(0, 16) : '',
      stops: flight.stops,
      basePrice: flight.basePrice,
      availableSeats: flight.availableSeats,
      durationMins: flight.durationMins,
      status: flight.status
    });
    this.showFlightModal = true;
  }

  public closeFlightModal(): void {
    this.showFlightModal = false;
    this.editingFlightId = null;
  }

  public submitFlight(): void {
    if (this.flightForm.invalid) {
      this.flightForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    const raw = this.flightForm.value;

    let dep = raw.departureTs;
    if (dep && dep.length === 16) dep += ':00';

    let arr = raw.arrivalTs;
    if (arr && arr.length === 16) arr += ':00';

    const payload = {
      flightNumber: raw.flightNumber,
      airlineCode: raw.airlineCode,
      fromAirport: raw.fromAirport,
      toAirport: raw.toAirport,
      aircraftId: raw.aircraftId ? Number(raw.aircraftId) : null,
      departureTs: dep,
      arrivalTs: arr,
      stops: Number(raw.stops || 0),
      basePrice: Number(raw.basePrice),
      availableSeats: Number(raw.availableSeats || 180),
      durationMins: Number(raw.durationMins || 120),
      status: raw.status || 'SCHEDULED'
    };

    if (this.editingFlightId) {
      this.flightsService.updateFlight(this.editingFlightId, payload).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeFlightModal();
          this.successMessage = 'Flight updated successfully!';
          this.loadAllData(true);
        },
        error: (err) => this.handleError(err)
      });
    } else {
      this.flightsService.addFlight(payload).subscribe({
        next: () => {
          this.isLoading = false;
          this.closeFlightModal();
          this.successMessage = 'Flight added successfully!';
          this.loadAllData(true);
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  public confirmDelete(flight: FlightResponseDTO): void {
    this.confirmModal = {
      show: true,
      title: 'Delete Flight',
      message: `Are you sure you want to delete flight "${flight.flightNumber}" (${flight.fromAirport} ➔ ${flight.toAirport})?`,
      onConfirm: () => {
        this.isLoading = true;
        this.flightsService.deleteFlight(flight.flightId).subscribe({
          next: () => {
            this.isLoading = false;
            this.confirmModal.show = false;
            this.successMessage = 'Flight deleted successfully!';
            this.loadAllData(true);
          },
          error: (err) => this.handleError(err)
        });
      }
    };
  }

  public openStatusModal(flight: FlightResponseDTO): void {
    this.statusUpdateFlight = flight;
    this.statusForm.patchValue({ status: flight.status });
    this.showStatusModal = true;
  }

  public closeStatusModal(): void {
    this.showStatusModal = false;
    this.statusUpdateFlight = null;
  }

  public submitStatusUpdate(): void {
    if (!this.statusUpdateFlight) return;
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.flightsService.updateFlightStatus(this.statusUpdateFlight.flightId, this.statusForm.value).subscribe({
      next: () => {
        this.isLoading = false;
        this.showStatusModal = false;
        this.successMessage = `Flight ${this.statusUpdateFlight?.flightNumber} status updated to ${this.statusForm.value.status}!`;
        this.loadAllData(true);
      },
      error: (err) => this.handleError(err)
    });
  }

  public openFlightDetailModal(flight: FlightResponseDTO): void {
    this.selectedFlightDetail = flight;
    this.showFlightDetailModal = true;
  }

  public closeFlightDetailModal(): void {
    this.showFlightDetailModal = false;
    this.selectedFlightDetail = null;
  }

  public getAirportLabel(code: string): string {
    const found = this.airports.find((a) => a.airportCode === code);
    if (!found) return code;
    const apName = found.name || found.airportName || '';
    return `${found.city} (${found.airportCode}) - ${apName}`;
  }

  public getAirlineLabel(code: string): string {
    const found = this.airlines.find((a) => a.airlineCode === code);
    if (!found) return code;
    return `${found.airlineName} (${found.airlineCode})`;
  }

  private routeValidator(group: AbstractControl): ValidationErrors | null {
    const from = group.get('fromAirport')?.value;
    const to = group.get('toAirport')?.value;
    if (from && to && from === to) {
      return { sameAirport: true };
    }
    return null;
  }

  private dateValidator(group: AbstractControl): ValidationErrors | null {
    const dep = group.get('departureTs')?.value;
    const arr = group.get('arrivalTs')?.value;
    if (dep && arr && new Date(arr) <= new Date(dep)) {
      return { invalidArrival: true };
    }
    return null;
  }

  private handleError(err: any): void {
    this.isLoading = false;
    if (err.status === 403) {
      this.errorMessage = 'You do not have permission to perform this action.';
    } else if (err.status === 409) {
      this.errorMessage = err.error?.message || 'Flight schedule conflict.';
    } else if (err.status === 0) {
      this.errorMessage = 'Unable to connect to the server.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process flight request.';
    }
  }
}
