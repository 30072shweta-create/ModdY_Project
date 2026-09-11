import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { interval, Subscription, forkJoin } from 'rxjs';

import { BookingsService } from '../../core/services/bookings.service';
import { PassengersService } from '../../core/services/passengers.service';
import { SeatLocksService } from '../../core/services/seat-locks.service';
import { PaymentsService } from '../../core/services/payments.service';
import { PricingService } from '../../core/services/pricing.service';
import { FlightsService } from '../../core/services/flights.service';
import { CouponsService } from '../../core/services/coupons.service';

import {
  BookingResponseDTO,
  PassengerResponseDTO,
  CabinClass
} from '../../core/models/booking.model';
import { FlightResponseDTO } from '../../core/models/flight.model';
import { UISeat, SeatLockResponseDTO } from '../../core/models/seat.model';
import { FareBreakdownDTO } from '../../core/models/pricing.model';
import { PaymentResponseDTO, RazorpayOptions } from '../../core/models/payment.model';
import { environment } from '../../../environments/environment';

export type BookingStep = 'passengers' | 'seats' | 'addons' | 'payment' | 'confirmation';

@Component({
  selector: 'app-booking-flow',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './booking-flow.component.html',
  styleUrls: ['./booking-flow.component.scss']
})
export class BookingFlowComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  public router = inject(Router);

  private bookingsService = inject(BookingsService);
  private passengersService = inject(PassengersService);
  private seatLocksService = inject(SeatLocksService);
  private paymentsService = inject(PaymentsService);
  private pricingService = inject(PricingService);
  private flightsService = inject(FlightsService);
  private couponsService = inject(CouponsService);

  public currentStep: BookingStep = 'passengers';
  public flightId!: number;
  public flight: FlightResponseDTO | null = null;
  public booking: BookingResponseDTO | null = null;
  public passengers: PassengerResponseDTO[] = [];
  public fareBreakdown: FareBreakdownDTO | null = null;
  public completedPayment: PaymentResponseDTO | null = null;
  public Math = Math;

  public isLoading = false;
  public errorMessage = '';
  public couponCode = '';
  public couponMessage = '';
  public isCouponValid = false;

  // Passenger Form
  public passengerForm: FormGroup = this.fb.group({
    passengers: this.fb.array([])
  });

  // Seat Lock Map
  public seatMap: UISeat[] = [];
  public selectedSeatsMap: Map<number, string> = new Map(); // passengerId -> seatNumber
  public activeLocksMap: Map<string, SeatLockResponseDTO> = new Map(); // seatNumber -> LockDTO
  public remainingTimeSeconds = 600; // 10 minutes lock timer
  private timerSubscription?: Subscription;

  public ngOnInit(): void {
    this.flightId = Number(this.route.snapshot.queryParams['flightId']);
    if (!this.flightId) {
      this.errorMessage = 'No flight selected for booking.';
      return;
    }

    this.loadFlight(this.flightId);
    this.initPassengerForm(1);
    this.paymentsService.loadRazorpayScript();
  }

  public ngOnDestroy(): void {
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
    }
  }

  public isPastStep(step: BookingStep): boolean {
    const order: BookingStep[] = ['passengers', 'seats', 'addons', 'payment', 'confirmation'];
    const currentIdx = order.indexOf(this.currentStep);
    const targetIdx = order.indexOf(step);
    return currentIdx > targetIdx;
  }

  private loadFlight(flightId: number): void {
    this.flightsService.getFlightById(flightId).subscribe({
      next: (f) => this.flight = f,
      error: () => this.errorMessage = 'Failed to load flight details.'
    });
  }

  public get passengerFormArray(): FormArray {
    return this.passengerForm.get('passengers') as FormArray;
  }

  public initPassengerForm(count: number): void {
    this.passengerFormArray.clear();
    for (let i = 0; i < count; i++) {
      this.passengerFormArray.push(this.fb.group({
        firstName: ['', [Validators.required]],
        lastName: ['', [Validators.required]],
        age: [25, [Validators.required, Validators.min(1)]],
        gender: ['MALE', [Validators.required]],
        passportNumber: ['']
      }));
    }
  }

  public addPassengerField(): void {
    if (this.passengerFormArray.length < 9) {
      this.passengerFormArray.push(this.fb.group({
        firstName: ['', [Validators.required]],
        lastName: ['', [Validators.required]],
        age: [25, [Validators.required, Validators.min(1)]],
        gender: ['MALE', [Validators.required]],
        passportNumber: ['']
      }));
    }
  }

  public removePassengerField(index: number): void {
    if (this.passengerFormArray.length > 1) {
      this.passengerFormArray.removeAt(index);
    }
  }

  // STEP 1: Submit Passengers & Create Booking
  public submitPassengers(): void {
    if (this.passengerForm.invalid) {
      this.passengerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // Create Draft Booking
    this.bookingsService.createBooking({
      flightIds: [this.flightId],
      cabinClass: 'ECONOMY'
    }).subscribe({
      next: (b) => {
        this.booking = b;
        this.savePassengers(b.bookingId);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to create booking.';
      }
    });
  }

  private savePassengers(bookingId: number): void {
    const rawList = this.passengerForm.value.passengers;
    const requests = rawList.map((p: any) => {
      const ageNum = Number(p.age || 25);
      const dobDate = new Date();
      dobDate.setFullYear(dobDate.getFullYear() - ageNum);
      const dobStr = p.dateOfBirth || dobDate.toISOString().split('T')[0];

      return this.passengersService.addPassenger(bookingId, {
        firstName: p.firstName,
        lastName: p.lastName,
        age: ageNum,
        gender: p.gender || 'MALE',
        passportNumber: p.passportNumber || '',
        dateOfBirth: dobStr
      });
    });

    forkJoin<PassengerResponseDTO[]>(requests).subscribe({
      next: (savedPassengers) => {
        this.passengers = savedPassengers;
        this.isLoading = false;
        this.generateSeatMap();
        this.currentStep = 'seats';
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || (typeof err.error === 'string' ? err.error : 'Failed to save passenger details.');
      }
    });
  }

  public activePassengerIndex = 0;

  public get activePassenger(): PassengerResponseDTO | null {
    return this.passengers[this.activePassengerIndex] || null;
  }

  public selectPassengerTab(index: number): void {
    if (index >= 0 && index < this.passengers.length) {
      this.activePassengerIndex = index;
    }
  }

  public get requiredSeatsCount(): number {
    return this.passengers ? this.passengers.length : 0;
  }

  public get assignedSeatsCount(): number {
    return this.selectedSeatsMap ? this.selectedSeatsMap.size : 0;
  }

  public get allSeatsAssigned(): boolean {
    if (!this.passengers || this.passengers.length === 0) return false;
    return this.passengers.every((passenger) => 
       this.selectedSeatsMap.has(passenger.passengerId)
    );
  }

  public seatMapByNum: Map<string, UISeat> = new Map();

  public getSeatByNum(seatNum: string): UISeat | undefined {
    return this.seatMapByNum.get(seatNum);
  }

  // STEP 2: Seat Map & Redis Locking
  public generateSeatMap(): void {
    const rows = 10;
    const cols = ['A', 'B', 'C', 'D', 'E', 'F'];
    this.seatMap = [];
    this.seatMapByNum.clear();

    for (let r = 1; r <= rows; r++) {
      for (const c of cols) {
        const seatNum = `${r}${c}`;
        const isBiz = r <= 2;
        const seatObj: UISeat = {
          seatNumber: seatNum,
          rowNumber: r,
          columnLetter: c,
          cabinClass: isBiz ? 'BUSINESS' : 'ECONOMY',
          price: isBiz ? (this.flight?.basePrice || 5000) * 1.5 : (this.flight?.basePrice || 3000),
          isLocked: false,
          isOccupied: false,
          isSelected: false,
          isMine: false
        };
        this.seatMap.push(seatObj);
        this.seatMapByNum.set(seatNum, seatObj);
      }
    }

    this.loadSeatMapAvailability();
  }

  public loadSeatMapAvailability(): void {
    if (!this.flightId) return;

    this.seatLocksService.getOccupiedSeatsForFlight(this.flightId).subscribe({
      next: (occupiedList: any[]) => {
        const occupiedMap = new Map<string, any>();
        (occupiedList || []).forEach((item) => {
          if (item && item.seatNumber) {
            occupiedMap.set(item.seatNumber, item);
          }
        });

        const currentBookingId = Number(this.booking?.bookingId);
        const myPassengerIds = new Set((this.passengers || []).map((p) => Number(p.passengerId)));
        const mySelectedSeatNumbers = new Set<String>(Array.from(this.selectedSeatsMap.values()));

        this.seatMap.forEach((seat) => {
          const occupied = occupiedMap.get(seat.seatNumber);
          const isLocallySelected = mySelectedSeatNumbers.has(seat.seatNumber);

          if (occupied) {
            const occBookingId = occupied.bookingId ? Number(occupied.bookingId) : null;
            const occPassengerId = occupied.passengerId ? Number(occupied.passengerId) : null;

            const isMyBooking = occBookingId !== null && occBookingId === currentBookingId;
            const isMyPassenger = occPassengerId !== null && myPassengerIds.has(occPassengerId);

            const belongsToMe = isMyBooking || isMyPassenger || isLocallySelected;

            if (occupied.status === 'BOOKED' && !belongsToMe) {
              seat.isOccupied = true;
              seat.isLocked = false;
              seat.isSelected = false;
              seat.isMine = false;
            } else if (belongsToMe) {
              seat.isOccupied = false;
              seat.isLocked = false;
              seat.isSelected = true;
              seat.isMine = true;
              if (occPassengerId) {
                seat.lockedByPassengerId = occPassengerId;
                this.selectedSeatsMap.set(occPassengerId, seat.seatNumber);
              }
            } else {
              seat.isOccupied = true;
              seat.isLocked = true;
              seat.isSelected = false;
              seat.isMine = false;
            }
          } else if (isLocallySelected) {
            let foundPassengerId: number | undefined;
            this.selectedSeatsMap.forEach((sNum, pId) => {
              if (sNum === seat.seatNumber) {
                foundPassengerId = pId;
              }
            });

            seat.isOccupied = false;
            seat.isLocked = false;
            seat.isSelected = true;
            seat.isMine = true;
            seat.lockedByPassengerId = foundPassengerId;
          } else {
            seat.isOccupied = false;
            seat.isLocked = false;
            seat.isSelected = false;
            seat.isMine = false;
          }
        });

        if (this.selectedSeatsMap.size > 0) {
          this.startLockCountdown();
        }
      },
      error: () => {}
    });
  }

  public isLockingSeat = false;
  public pendingSeatNumber: string | null = null;

  public handleSeatClick(seat: UISeat): void {
    console.log(`[SEAT-LOCK] Seat clicked: ${seat.seatNumber}, isLockingSeat: ${this.isLockingSeat}, isLoading: ${this.isLoading}`);

    // Prevent clicks while locking or loading is in progress, or on seats occupied by others
    if (this.isLockingSeat || this.isLoading || (seat.isOccupied && !seat.isMine)) {
      return;
    }

    const currentPass = this.activePassenger;
    if (!currentPass) return;

    const passengerId = currentPass.passengerId;

    // 1. Releasing own selected seat
    if (seat.isSelected && seat.isMine && seat.lockedByPassengerId === passengerId) {
      console.log(`[SEAT-LOCK] Releasing seat ${seat.seatNumber} for passenger ${passengerId}`);
      this.isLockingSeat = true;
      this.pendingSeatNumber = seat.seatNumber;
      seat.isPending = true;

      this.seatLocksService.releaseSeatLock(this.flightId, seat.seatNumber).subscribe({
        next: () => {
          console.log(`[SEAT-LOCK] Released seat ${seat.seatNumber} successfully`);
          this.isLockingSeat = false;
          this.pendingSeatNumber = null;
          seat.isPending = false;

          seat.isSelected = false;
          seat.isMine = false;
          seat.isOccupied = false;
          seat.isLocked = false;
          seat.lockedByPassengerId = undefined;

          this.selectedSeatsMap.delete(passengerId);
          this.activeLocksMap.delete(seat.seatNumber);
          console.log(`[SEAT-LOCK] Current selectedSeatsMap:`, Array.from(this.selectedSeatsMap.entries()));
        },
        error: (err) => {
          console.error(`[SEAT-LOCK] Failed to release seat ${seat.seatNumber}`, err);
          this.isLockingSeat = false;
          this.pendingSeatNumber = null;
          seat.isPending = false;
          this.errorMessage = err.error?.message || 'Failed to release seat lock.';
        }
      });
      return;
    }

    // 2. Switching to a new seat for the same passenger
    const previousSeatNumber = this.selectedSeatsMap.get(passengerId);
    if (previousSeatNumber && previousSeatNumber !== seat.seatNumber) {
      console.log(`[SEAT-LOCK] Switching passenger ${passengerId} from ${previousSeatNumber} to ${seat.seatNumber}`);
      this.isLockingSeat = true;
      this.pendingSeatNumber = seat.seatNumber;
      seat.isPending = true;

      this.seatLocksService.releaseSeatLock(this.flightId, previousSeatNumber).subscribe({
        next: () => {
          this.selectedSeatsMap.delete(passengerId);
          const previousSeat = this.seatMapByNum.get(previousSeatNumber);
          if (previousSeat) {
            previousSeat.isSelected = false;
            previousSeat.isMine = false;
            previousSeat.isOccupied = false;
            previousSeat.isLocked = false;
            previousSeat.lockedByPassengerId = undefined;
          }
          this.lockNewSeatOptimistically(seat, currentPass);
        },
        error: () => {
          this.lockNewSeatOptimistically(seat, currentPass);
        }
      });
      return;
    }

    // 3. Locking new seat for passenger who doesn't have a seat yet
    this.lockNewSeatOptimistically(seat, currentPass);
  }

  private lockNewSeatOptimistically(seat: UISeat, passenger: PassengerResponseDTO): void {
    const segmentId = this.booking?.segments[0]?.segmentId;
    if (!segmentId) {
      this.errorMessage = 'No booking segment found.';
      return;
    }

    const passengerId = passenger.passengerId;
    console.log(`[SEAT-LOCK] Lock request started for seat: ${seat.seatNumber}, passengerId: ${passengerId}`);

    this.isLockingSeat = true;
    this.pendingSeatNumber = seat.seatNumber;
    this.errorMessage = '';

    // OPTIMISTIC SELECTION UPDATE
    seat.isPending = true;
    seat.isSelected = true;
    seat.isMine = true;
    seat.isOccupied = false;
    seat.isLocked = false;
    seat.lockedByPassengerId = passengerId;
    this.selectedSeatsMap.set(passengerId, seat.seatNumber);
    console.log(`[SEAT-LOCK] Optimistic selectedSeatsMap updated:`, Array.from(this.selectedSeatsMap.entries()));

    this.seatLocksService.lockSpecificSeat(this.flightId, segmentId, seat.seatNumber, passengerId).subscribe({
      next: (lockRes) => {
        console.log(`[SEAT-LOCK] Lock request success for seat: ${seat.seatNumber}`, lockRes);
        this.isLockingSeat = false;
        this.pendingSeatNumber = null;

        seat.isPending = false;
        seat.isSelected = true;
        seat.isMine = true;
        seat.isOccupied = false;
        seat.isLocked = false;
        seat.lockedByPassengerId = passengerId;

        this.selectedSeatsMap.set(passengerId, seat.seatNumber);
        this.activeLocksMap.set(seat.seatNumber, lockRes);
        this.startLockCountdown();

        // Advance to next unassigned passenger if applicable
        const nextIndex = this.passengers.findIndex((p) => !this.selectedSeatsMap.has(p.passengerId));
        if (nextIndex !== -1) {
          this.activePassengerIndex = nextIndex;
        }
      },
      error: (err) => {
        console.error(`[SEAT-LOCK] Lock request failed for seat: ${seat.seatNumber}`, err);
        this.isLockingSeat = false;
        this.pendingSeatNumber = null;

        // ROLLBACK OPTIMISTIC SELECTION
        seat.isPending = false;
        seat.isSelected = false;
        seat.isMine = false;
        seat.isOccupied = false;
        seat.isLocked = false;
        seat.lockedByPassengerId = undefined;

        this.selectedSeatsMap.delete(passengerId);
        this.activeLocksMap.delete(seat.seatNumber);
        console.log(`[SEAT-LOCK] Rollback selectedSeatsMap:`, Array.from(this.selectedSeatsMap.entries()));

        this.errorMessage = err.error?.message || `Seat ${seat.seatNumber} is no longer available. Please select another seat.`;
      }
    });
  }

  private startLockCountdown(): void {
    if (this.timerSubscription) this.timerSubscription.unsubscribe();
    this.remainingTimeSeconds = 600; // 10 mins

    this.timerSubscription = interval(1000).subscribe(() => {
      this.remainingTimeSeconds--;
      if (this.remainingTimeSeconds <= 0) {
        if (this.timerSubscription) this.timerSubscription.unsubscribe();
        this.errorMessage = 'Seat lock expired. Please select your seats again.';
        this.selectedSeatsMap.clear();
        this.activeLocksMap.clear();
        this.loadSeatMapAvailability();
      }
    });
  }

  public extraBaggageSelected = false;
  public gourmetMealSelected = false;
  public travelInsuranceSelected = false;

  public proceedToAddons(): void {
    if (!this.allSeatsAssigned) {
      this.errorMessage = 'Please select a seat for all passengers.';
      return;
    }

    this.errorMessage = '';
    this.currentStep = 'addons';
  }

  // STEP 3: Add-ons & Fare Calculation
  public proceedToPayment(): void {
    if (!this.allSeatsAssigned) {
      this.errorMessage = 'Please select a seat for all passengers.';
      return;
    }
    if (!this.booking) {
      this.errorMessage = 'No active booking found. Please complete passenger details.';
      return;
    }

    this.errorMessage = '';
    this.currentStep = 'payment';
    this.calculateFare();
  }

  public calculateFare(): void {
    if (!this.flight) return;
    this.isLoading = true;

    console.log('[FARE-CALCULATE] Selected add-ons:', {
      extraBaggageSelected: this.extraBaggageSelected,
      gourmetMealSelected: this.gourmetMealSelected,
      travelInsuranceSelected: this.travelInsuranceSelected
    });

    this.pricingService.calculateFare({
      flightId: this.flightId,
      bookingId: this.booking?.bookingId,
      cabinClass: 'ECONOMY',
      passengerCount: (this.passengers && this.passengers.length > 0) ? this.passengers.length : 1,
      travelDate: this.flight.departureTs,
      couponCode: this.isCouponValid ? this.couponCode : undefined,
      extraBaggage: this.extraBaggageSelected,
      gourmetMeal: this.gourmetMealSelected,
      travelInsurance: this.travelInsuranceSelected
    }).subscribe({
      next: (fare) => {
        console.log('[FARE-CALCULATE] Fare response:', fare);
        this.fareBreakdown = fare;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('[FARE-CALCULATE] Failed to calculate fare from backend:', err);
        const passCount = (this.passengers && this.passengers.length > 0) ? this.passengers.length : 1;
        const base = (this.flight?.basePrice || 3000) * passCount;
        const extraBaggageFee = this.extraBaggageSelected ? 1200 : 0;
        const gourmetMealFee = this.gourmetMealSelected ? 450 : 0;
        const travelInsuranceFee = this.travelInsuranceSelected ? 299 : 0;
        const taxAmt = 500 * passCount;
        const disc = this.isCouponValid ? 500 : 0;
        const total = base + taxAmt + extraBaggageFee + gourmetMealFee + travelInsuranceFee - disc;

        this.fareBreakdown = {
          flightId: this.flightId,
          cabinClass: 'ECONOMY',
          baseFare: base,
          taxAmount: taxAmt,
          extraBaggageFee: extraBaggageFee,
          gourmetMealFee: gourmetMealFee,
          travelInsuranceFee: travelInsuranceFee,
          discountAmount: disc,
          couponDiscount: disc,
          totalFare: total,
          finalPrice: total,
          currency: 'INR'
        };
      }
    });
  }

  public applyCoupon(): void {
    const code = this.couponCode.trim();
    if (!code) return;
    this.couponMessage = 'Validating coupon code...';

    const currentSubtotal = this.fareBreakdown?.baseFare || 1000;

    this.couponsService.validateCoupon(code, currentSubtotal).subscribe({
      next: (validCoupon) => {
        console.log('[COUPON-VALIDATE] Valid coupon from DB:', validCoupon);
        this.isCouponValid = true;
        this.couponCode = validCoupon.couponCode;
        this.couponMessage = 'Coupon applied successfully!';
        this.calculateFare();
      },
      error: (err) => {
        console.error('[COUPON-VALIDATE] Coupon validation failed:', err);
        this.isCouponValid = false;
        const errMsg = err.error?.message || (typeof err.error === 'string' ? err.error : 'Invalid or expired coupon code.');
        this.couponMessage = errMsg;
        this.calculateFare();
      }
    });
  }

  // STEP 4: Razorpay Payment & Verification
  public initiatePayment(): void {
    if (!this.booking) return;

    const finalAmount = this.fareBreakdown?.finalPrice || this.fareBreakdown?.totalFare;
    if (!finalAmount || finalAmount <= 0) {
      this.errorMessage = 'Fare is still being calculated. Please try again.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    console.log('[PAYMENT] Creating payment order for bookingId:', this.booking.bookingId, 'with final amount:', finalAmount);

    // 1. Create Payment Order on Spring Boot backend
    this.paymentsService.createPaymentOrder({
      bookingId: this.booking.bookingId,
      paymentMethod: 'RAZORPAY'
    }).subscribe({
      next: (orderRes) => {
        console.log('[PAYMENT] Payment order created successfully:', orderRes);
        this.isLoading = false;
        this.launchRazorpayCheckout(orderRes);
      },
      error: (err) => {
        this.isLoading = false;
        console.error('[PAYMENT] Failed to create payment order:', err);
        this.errorMessage = err.error?.message || (typeof err.error === 'string' ? err.error : 'Failed to create payment order.');
      }
    });
  }

  private launchRazorpayCheckout(orderRes: PaymentResponseDTO): void {
    const orderAmountInPaise = Math.round(Number(orderRes.amount) * 100);
    console.log('[RAZORPAY] Launching checkout modal. Order ID:', orderRes.razorpayOrderId, 'Amount (paise):', orderAmountInPaise);

    const razorpayOptions: RazorpayOptions = {
      key: environment.razorpayKeyId,
      amount: orderAmountInPaise,
      currency: orderRes.currency || 'INR',
      name: 'SkyRoute Flight System',
      description: `Flight Booking PNR: ${this.booking?.bookingCode}`,
      order_id: orderRes.razorpayOrderId || '',
      handler: (res: any) => {
        console.log('[RAZORPAY] Payment success handler callback received:', res);
        this.verifyPaymentOnBackend(res.razorpay_order_id, res.razorpay_payment_id, res.razorpay_signature);
      },
      modal: {
        ondismiss: () => {
          console.log('[RAZORPAY] Modal closed/dismissed by user');
          this.isLoading = false;
        }
      },
      prefill: {
        name: `${this.passengers[0]?.firstName || ''} ${this.passengers[0]?.lastName || ''}`,
        email: 'user@example.com'
      },
      theme: { color: '#0284C7' }
    };

    this.paymentsService.openRazorpayModal(razorpayOptions);
  }

  // 2. Verify Payment Signature on Spring Boot Backend
  private verifyPaymentOnBackend(orderId: string, paymentId: string, signature: string): void {
    console.log('[PAYMENT-VERIFY] Verifying payment signature on backend:', { orderId, paymentId, signature });
    this.isLoading = true;

    this.paymentsService.verifyPayment({
      razorpayOrderId: orderId,
      razorpayPaymentId: paymentId,
      razorpaySignature: signature
    }).subscribe({
      next: (verifiedPayment) => {
        console.log('[PAYMENT-VERIFY] Payment verified successfully:', verifiedPayment);
        this.isLoading = false;
        this.completedPayment = verifiedPayment;

        // Confirm booking on backend
        if (this.booking) {
          this.bookingsService.confirmBooking(this.booking.bookingId).subscribe({
            next: (confirmedB) => {
              console.log('[BOOKING-CONFIRM] Booking confirmed:', confirmedB);
              this.booking = confirmedB;
              this.currentStep = 'confirmation';
            },
            error: (err) => {
              console.error('[BOOKING-CONFIRM] Error confirming booking:', err);
              this.currentStep = 'confirmation';
            }
          });
        } else {
          this.currentStep = 'confirmation';
        }
      },
      error: (err) => {
        this.isLoading = false;
        console.error('[PAYMENT-VERIFY] Payment verification failed:', err);
        this.errorMessage = err.error?.message || 'Payment verification failed on backend. Please contact support.';
      }
    });
  }
}
