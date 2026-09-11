import { Component, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  public authService = inject(AuthService);
  private router = inject(Router);
  public isMobileMenuOpen = false;
  public isScrolled = false;

  @HostListener('window:scroll')
  onScroll(): void {
    this.isScrolled = window.scrollY > 10;
  }

  public toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  public closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  public logout(): void {
    this.isMobileMenuOpen = false;
    this.authService.logout();
  }

  public scrollToAbout(): void {
    if (this.router.url !== '/' && !this.router.url.startsWith('/#')) {
      this.router.navigate(['/']).then(() => {
        setTimeout(() => {
          const el = document.getElementById('about-section');
          if (el) el.scrollIntoView({ behavior: 'smooth' });
        }, 150);
      });
    } else {
      const el = document.getElementById('about-section');
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
