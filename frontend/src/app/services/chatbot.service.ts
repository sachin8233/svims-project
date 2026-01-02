import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatbotRequest {
  message: string;
  username?: string;
  role?: string;
}

export interface ChatbotResponse {
  response: string;
  role: string;
  data?: any[];
  suggestion?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatbotService {
  private apiUrl = 'http://localhost:8080/api/chatbot';

  constructor(private http: HttpClient) {}

  /**
   * Send a message to the chatbot
   */
  sendMessage(message: string): Observable<ChatbotResponse> {
    const headers = this.getHeaders();
    const request: ChatbotRequest = { message };
    
    return this.http.post<ChatbotResponse>(`${this.apiUrl}/chat`, request, { headers });
  }

  /**
   * Get help information
   */
  getHelp(): Observable<ChatbotResponse> {
    const headers = this.getHeaders();
    return this.http.get<ChatbotResponse>(`${this.apiUrl}/help`, { headers });
  }

  /**
   * Get authorization headers with JWT token
   */
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }
}

