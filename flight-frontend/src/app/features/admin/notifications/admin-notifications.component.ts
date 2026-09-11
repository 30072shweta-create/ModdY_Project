import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { NotificationsService } from '../../../core/services/notifications.service';
import { UsersService } from '../../../core/services/users.service';
import { NotificationResponseDTO } from '../../../core/models/notification.model';
import { UserResponseDTO } from '../../../core/models/auth.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-notifications',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-notifications.component.html',
  styleUrls: ['./admin-notifications.component.scss']
})
export class AdminNotificationsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private notificationsService = inject(NotificationsService);
  private usersService = inject(UsersService);

  public notificationList: NotificationResponseDTO[] = [];
  public usersList: UserResponseDTO[] = [];
  public search = '';
  public isLoading = false;
  public errorMessage = '';
  public successMessage = '';

  public showModal = false;

  public confirmModal = {
    show: false,
    title: '',
    message: '',
    onConfirm: () => {}
  };

  public notificationForm: FormGroup = this.fb.group({
    userId: ['', [Validators.required]],
    bookingId: [''],
    recipientEmail: ['', [Validators.required, Validators.email]],
    type: ['SYSTEM_ALERT', [Validators.required]],
    message: ['', [Validators.required, Validators.maxLength(500)]]
  });

  public ngOnInit(): void {
    this.loadNotifications();
    this.loadUsers();
  }

  public loadNotifications(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.notificationsService.getAllNotifications().subscribe({
      next: (data) => {
        this.notificationList = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public loadUsers(): void {
    this.usersService.getAllUsers().subscribe({
      next: (data) => {
        this.usersList = data || [];
      },
      error: (err) => console.error('Failed to load users for notification target', err)
    });
  }

  public get filteredNotifications(): NotificationResponseDTO[] {
    return this.notificationList.filter((n) => {
      if (!this.search) return true;
      const term = this.search.toLowerCase();
      return (
        n.notificationId.toString().includes(term) ||
        (n.recipientEmail || '').toLowerCase().includes(term) ||
        (n.type || '').toLowerCase().includes(term) ||
        (n.message || '').toLowerCase().includes(term)
      );
    });
  }

  public onUserSelect(event: any): void {
    const selectedId = Number(event.target.value);
    const user = this.usersList.find((u) => u.userId === selectedId);
    if (user) {
      this.notificationForm.patchValue({ recipientEmail: user.email });
    }
  }

  public isSending = false;
  public modalErrorMessage = '';

  public openAddModal(): void {
    this.modalErrorMessage = '';
    this.isSending = false;
    const defaultUser = this.usersList.length > 0 ? this.usersList[0] : null;
    this.notificationForm.reset({
      userId: defaultUser ? defaultUser.userId : '',
      recipientEmail: defaultUser ? defaultUser.email : '',
      type: 'SYSTEM_ALERT',
      message: ''
    });
    this.showModal = true;
  }

  public closeModal(): void {
    this.showModal = false;
    this.modalErrorMessage = '';
    this.isSending = false;
  }

  public submitNotification(): void {
    this.modalErrorMessage = '';

    let formVal = this.notificationForm.value;
    let userId = formVal.userId ? Number(formVal.userId) : null;
    let recipientEmail = (formVal.recipientEmail || '').trim();

    // Auto-resolve user if missing
    if (!userId && recipientEmail) {
      const match = this.usersList.find(u => u.email.toLowerCase() === recipientEmail.toLowerCase());
      if (match) {
        userId = match.userId;
      } else if (this.usersList.length > 0) {
        userId = this.usersList[0].userId;
      }
    } else if (userId && !recipientEmail) {
      const match = this.usersList.find(u => u.userId === userId);
      if (match) {
        recipientEmail = match.email;
      }
    }

    const message = (formVal.message || '').trim();
    if (!message) {
      this.modalErrorMessage = 'Notification message is required.';
      this.notificationForm.get('message')?.markAsTouched();
      return;
    }

    if (!recipientEmail) {
      this.modalErrorMessage = 'Recipient email is required.';
      this.notificationForm.get('recipientEmail')?.markAsTouched();
      return;
    }

    if (!userId) {
      userId = this.usersList[0]?.userId || 1;
    }

    this.isSending = true;
    this.errorMessage = '';
    this.successMessage = '';

    const dto: any = {
      userId: userId,
      recipientEmail: recipientEmail,
      type: formVal.type || 'SYSTEM_ALERT',
      channel: 'EMAIL',
      message: message
    };
    if (formVal.bookingId) {
      dto.bookingId = Number(formVal.bookingId);
    }

    this.notificationsService.createNotification(dto).subscribe({
      next: (res) => {
        this.isSending = false;
        this.closeModal();
        this.successMessage = `Notification sent to ${recipientEmail} successfully!`;
        this.autoDismissToast();
        this.loadNotifications(true);
      },
      error: (err) => {
        this.isSending = false;
        this.modalErrorMessage = err.error?.message || err.error?.detail || 'Failed to dispatch notification email.';
      }
    });
  }

  public confirmDelete(n: NotificationResponseDTO): void {
    this.confirmModal = {
      show: true,
      title: 'Delete Notification',
      message: `Are you sure you want to delete notification #${n.notificationId}?`,
      onConfirm: () => {
        this.isLoading = true;
        this.notificationsService.deleteNotification(n.notificationId).subscribe({
          next: () => {
            this.isLoading = false;
            this.confirmModal.show = false;
            this.successMessage = 'Notification deleted successfully!';
            this.autoDismissToast();
            this.loadNotifications(true);
          },
          error: (err) => this.handleError(err)
        });
      }
    };
  }

  private autoDismissToast(): void {
    setTimeout(() => {
      this.successMessage = '';
    }, 4000);
  }

  private handleError(err: any): void {
    this.isLoading = false;
    if (err.status === 403) {
      this.errorMessage = 'You do not have permission to perform this action.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process notification request.';
    }
  }
}
