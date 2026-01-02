import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { AuthService } from './services/auth.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'SVIMS - Smart Vendor & Invoice Management System';
  isChatbotOpen = false;
  showChatbotButton = false;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Validate session on app initialization
    this.validateSession();
    
    // Show chatbot button only on authenticated pages
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.checkAuthStatus();
    });
    
    this.checkAuthStatus();
  }

  private validateSession(): void {
    // Validate session if user appears to be logged in
    if (this.authService.isAuthenticated()) {
      this.authService.validateSession().subscribe(isValid => {
        if (!isValid) {
          // Session invalid, will be handled by auth service
          console.log('Session validation failed');
        }
      });
    }
  }

  checkAuthStatus(): void {
    const currentUser = this.authService.getCurrentUser();
    const currentPath = this.router.url;
    // Show button if user is logged in and not on auth page
    this.showChatbotButton = currentUser !== null && !currentPath.includes('/auth');
  }

  toggleChatbot(): void {
    this.isChatbotOpen = !this.isChatbotOpen;
  }

  closeChatbot(): void {
    this.isChatbotOpen = false;
  }
}

