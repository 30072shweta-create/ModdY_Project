import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationsService } from '../../core/services/notifications.service';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationResponseDTO } from '../../core/models/notification.model';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notifications-page">
      <div class="container">
        <div class="page-header">
          <h2>Notifications & Updates</h2>
          <p>Important messages regarding your flight bookings and schedule</p>
        </div>

        @if (isLoading) {
          <div class="card p-4 text-center">Loading notifications...</div>
        } @else if (notifications.length === 0) {
          <div class="card p-4 text-center">
            <p class="text-muted">No notifications at this time.</p>
          </div>
        } @else {
          <div class="notif-list">
            @for (n of notifications; track n.notificationId) {
              <div class="card notif-card">
                <div class="notif-header">
                  <span class="badge badge-info">{{ n.type }}</span>
                  <span class="time">{{ n.sentAt | date:'medium' }}</span>
                </div>
                <p class="msg">{{ n.message }}</p>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .notifications-page { padding: 3rem 0; }
    .page-header { margin-bottom: 2rem; }
    .notif-list { display: flex; flex-direction: column; gap: 1rem; }
    .notif-card { padding: 1.25rem; }
    .notif-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .time { font-size: 0.8rem; color: var(--text-muted); }
    .msg { font-size: 0.95rem; color: var(--text-main); }
  `]
})
export class NotificationsComponent implements OnInit {
  private notificationsService = inject(NotificationsService);
  private authService = inject(AuthService);

  public notifications: NotificationResponseDTO[] = [];
  public isLoading = true;

  public ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user?.userId) {
      this.notificationsService.getNotificationsByUser(user.userId).subscribe({
        next: (data) => {
          this.notifications = data;
          this.isLoading = false;
        },
        error: () => this.isLoading = false
      });
    } else {
      this.isLoading = false;
    }
  }
}
