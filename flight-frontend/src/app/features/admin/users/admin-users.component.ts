import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsersService } from '../../../core/services/users.service';
import { UserResponseDTO } from '../../../core/models/auth.model';
import { AdminSidebarComponent } from '../components/admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminSidebarComponent],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {
  private usersService = inject(UsersService);

  public userList: UserResponseDTO[] = [];
  public search = '';
  public roleFilter = 'ALL';
  public isLoading = false;
  public errorMessage = '';
  public successMessage = '';

  public selectedUser: UserResponseDTO | null = null;
  public showEditModal = false;
  public editRole = 'ROLE_USER';
  public editVerified = false;

  public ngOnInit(): void {
    this.loadUsers();
  }

  public loadUsers(preserveSuccessMessage = false): void {
    this.isLoading = true;
    this.errorMessage = '';
    if (!preserveSuccessMessage) {
      this.successMessage = '';
    }

    this.usersService.getAllUsers().subscribe({
      next: (data) => {
        this.userList = data || [];
        this.isLoading = false;
      },
      error: (err) => this.handleError(err)
    });
  }

  public get filteredUsers(): UserResponseDTO[] {
    return this.userList.filter((u) => {
      const matchesRole = this.roleFilter === 'ALL' || u.role === this.roleFilter;
      if (!matchesRole) return false;

      if (!this.search) return true;
      const term = this.search.toLowerCase();
      const fullName = `${u.firstName || ''} ${u.lastName || ''}`.toLowerCase();
      return (
        u.userId.toString().includes(term) ||
        u.email.toLowerCase().includes(term) ||
        fullName.includes(term) ||
        u.role.toLowerCase().includes(term)
      );
    });
  }

  public openEditModal(user: UserResponseDTO): void {
    this.selectedUser = user;
    this.editRole = user.role;
    this.editVerified = user.emailVerified;
    this.showEditModal = true;
  }

  public closeEditModal(): void {
    this.showEditModal = false;
    this.selectedUser = null;
  }

  public submitUserUpdate(): void {
    if (!this.selectedUser) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const userId = this.selectedUser.userId;

    this.usersService.updateUserRole(userId, this.editRole).subscribe({
      next: () => {
        this.usersService.updateUserStatus(userId, this.editVerified).subscribe({
          next: () => {
            this.isLoading = false;
            this.closeEditModal();
            this.successMessage = `User #${userId} updated successfully!`;
            this.loadUsers(true);
          },
          error: (err) => this.handleError(err)
        });
      },
      error: (err) => this.handleError(err)
    });
  }

  private handleError(err: any): void {
    this.isLoading = false;
    if (err.status === 403) {
      this.errorMessage = 'You do not have permission to perform this action.';
    } else {
      this.errorMessage = err.error?.message || err.error || 'Failed to process user request.';
    }
  }
}
