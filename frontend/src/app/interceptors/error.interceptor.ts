import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private router: Router, private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<any> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 403) {
          // Unauthorized or Forbidden - clear session and redirect
          this.authService.logout();
          this.router.navigate(['/auth'], { 
            queryParams: { 
              expired: 'true',
              message: 'Your session has expired or you do not have permission to access this resource.' 
            } 
          });
        }
        return throwError(() => error);
      })
    );
  }
}

