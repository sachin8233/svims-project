import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-auth',
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css']
})
export class AuthComponent implements OnInit {
  loginForm!: FormGroup;
  registerForm!: FormGroup;
  loginErrorMessage: string = '';
  registerErrorMessage: string = '';
  showPassword: { login: boolean; register: boolean; confirm: boolean } = {
    login: false,
    register: false,
    confirm: false
  };

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Check if redirected due to session expiration
    this.route.queryParams.subscribe(params => {
      if (params['expired'] === 'true') {
        this.loginErrorMessage = params['message'] || 'Your session has expired. Please login again.';
      }
    });

    // Clear any existing session data
    if (this.authService.isAuthenticated()) {
      this.authService.logout();
    }

    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });

    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(form: FormGroup): { [key: string]: any } | null {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      return { passwordMismatch: true };
    }
    return null;
  }

  togglePasswordVisibility(type: 'login' | 'register' | 'confirm'): void {
    this.showPassword[type] = !this.showPassword[type];
  }

  onLogin(): void {
    if (this.loginForm.valid) {
      this.loginErrorMessage = '';
      this.authService.login(this.loginForm.value).subscribe({
        next: () => {
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loginErrorMessage = err.error?.error || 'Invalid username or password';
          console.error('Login error:', err);
        }
      });
    }
  }

  onRegister(): void {
    if (this.registerForm.valid) {
      this.registerErrorMessage = '';
      const { confirmPassword, ...registerData } = this.registerForm.value;
      this.authService.register(registerData).subscribe({
        next: () => {
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.registerErrorMessage = err.error?.error || 'Registration failed. Please try again.';
          console.error('Registration error:', err);
        }
      });
    }
  }
}

