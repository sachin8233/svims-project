import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    // Check if user is authenticated and token is valid
    if (this.authService.isAuthenticated()) {
      // Check if token is expiring soon and show warning
      if (this.authService.isTokenExpiringSoon()) {
        const timeRemaining = this.authService.getTokenTimeRemaining();
        const minutes = Math.floor(timeRemaining / 60000);
        console.warn(`Session expiring in ${minutes} minutes`);
        // You can show a toast/notification here if needed
      }
      return true;
    }
    
    // Not authenticated, redirect to login
    this.router.navigate(['/auth'], { 
      queryParams: { 
        returnUrl: this.router.url 
      } 
    });
    return false;
  }
}

