import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';

import { HomeComponent } from './features/home/home.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { VerifyEmailComponent } from './features/auth/verify-email/verify-email.component';

import { FlightListComponent } from './features/flights/flight-list/flight-list.component';
import { FlightDetailComponent } from './features/flights/flight-detail/flight-detail.component';

import { BookingFlowComponent } from './features/booking/booking-flow.component';
import { MyBookingsComponent } from './features/bookings/my-bookings/my-bookings.component';
import { BookingDetailComponent } from './features/bookings/booking-detail/booking-detail.component';

import { ProfileComponent } from './features/profile/profile.component';
import { NotificationsComponent } from './features/notifications/notifications.component';
import { AdminDashboardComponent } from './features/admin/admin.component';
import { AdminFlightsComponent } from './features/admin/flights/admin-flights.component';
import { AdminAirlinesComponent } from './features/admin/airlines/admin-airlines.component';
import { AdminAirportsComponent } from './features/admin/airports/admin-airports.component';
import { AdminAircraftComponent } from './features/admin/aircraft/admin-aircraft.component';
import { AdminPricingComponent } from './features/admin/pricing/admin-pricing.component';
import { AdminCouponsComponent } from './features/admin/coupons/admin-coupons.component';
import { AdminHolidaysComponent } from './features/admin/holidays/admin-holidays.component';
import { AdminPricingRulesComponent } from './features/admin/pricing-rules/admin-pricing-rules.component';
import { AdminBookingsComponent } from './features/admin/bookings/admin-bookings.component';
import { AdminPaymentsComponent } from './features/admin/payments/admin-payments.component';
import { AdminUsersComponent } from './features/admin/users/admin-users.component';
import { AdminNotificationsComponent } from './features/admin/notifications/admin-notifications.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'verify-email', component: VerifyEmailComponent },

  { path: 'search', component: FlightListComponent },
  { path: 'flights', component: FlightListComponent },
  { path: 'flight/:id', component: FlightDetailComponent },

  { path: 'booking', component: BookingFlowComponent, canActivate: [authGuard] },
  { path: 'my-bookings', component: MyBookingsComponent, canActivate: [authGuard] },
  { path: 'booking/:id', component: BookingDetailComponent, canActivate: [authGuard] },

  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'notifications', component: NotificationsComponent, canActivate: [authGuard] },

  { path: 'admin', component: AdminDashboardComponent, canActivate: [adminGuard] },
  { path: 'admin/flights', component: AdminFlightsComponent, canActivate: [adminGuard] },
  { path: 'admin/airlines', component: AdminAirlinesComponent, canActivate: [adminGuard] },
  { path: 'admin/airports', component: AdminAirportsComponent, canActivate: [adminGuard] },
  { path: 'admin/aircraft', component: AdminAircraftComponent, canActivate: [adminGuard] },
  { path: 'admin/pricing', component: AdminPricingComponent, canActivate: [adminGuard] },
  { path: 'admin/coupons', component: AdminCouponsComponent, canActivate: [adminGuard] },
  { path: 'admin/holidays', component: AdminHolidaysComponent, canActivate: [adminGuard] },
  { path: 'admin/pricing-rules', component: AdminPricingRulesComponent, canActivate: [adminGuard] },
  { path: 'admin/bookings', component: AdminBookingsComponent, canActivate: [adminGuard] },
  { path: 'admin/payments', component: AdminPaymentsComponent, canActivate: [adminGuard] },
  { path: 'admin/users', component: AdminUsersComponent, canActivate: [adminGuard] },
  { path: 'admin/notifications', component: AdminNotificationsComponent, canActivate: [adminGuard] },

  { path: '**', redirectTo: '' }
];
