import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-sidebar.component.html',
  styleUrls: ['./admin-sidebar.component.scss']
})
export class AdminSidebarComponent {
  public router = inject(Router);
  public isSidebarCollapsed = false;
  public isMobileSidebarOpen = false;

  public toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  public toggleMobileSidebar(): void {
    this.isMobileSidebarOpen = !this.isMobileSidebarOpen;
  }

  public isActive(path: string): boolean {
    if (path === '/admin') {
      return this.router.url === '/admin';
    }
    return this.router.url.startsWith(path);
  }
}
