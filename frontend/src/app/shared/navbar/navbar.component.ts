import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  currentUser: any;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
  }

  hasRole(role: string): boolean {
    return this.authService.hasRole(role);
  }

  hasAdminRole(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }

  hasManagerRole(): boolean {
    return this.hasRole('ROLE_MANAGER') || this.hasAdminRole();
  }

  hasFinanceRole(): boolean {
    return this.hasRole('ROLE_FINANCE') || this.hasAdminRole();
  }

  hasUserRole(): boolean {
    return this.hasRole('ROLE_USER');
  }

  logout(): void {
    this.authService.logout();
  }

  navigateToDashboard(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/auth']);
    }
  }
}

