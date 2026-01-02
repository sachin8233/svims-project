import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User {
  id: number;
  username: string;
  email: string;
  isActive: boolean;
  roles: string[];
}

export interface UpdateUserRolesRequest {
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = 'http://localhost:8080/api/users';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }

  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  updateUserRoles(id: number, role: string): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}/roles`, { role });
  }

  toggleUserStatus(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/toggle-status`, {});
  }
}

