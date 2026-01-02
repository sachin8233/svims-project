import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked, Output, EventEmitter } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { ChatbotService, ChatbotResponse } from '../services/chatbot.service';
import { AuthService } from '../services/auth.service';

interface ChatMessage {
  text: string;
  isUser: boolean;
  timestamp: Date;
  suggestion?: string;
  data?: any[]; // Structured data from backend
}

@Component({
  selector: 'app-chatbot',
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css'],
  animations: [
    trigger('slideDown', [
      transition(':enter', [
        style({ height: '0', opacity: 0, overflow: 'hidden' }),
        animate('300ms ease-in', style({ height: '*', opacity: 1 }))
      ]),
      transition(':leave', [
        animate('300ms ease-out', style({ height: '0', opacity: 0, overflow: 'hidden' }))
      ])
    ])
  ]
})
export class ChatbotComponent implements OnInit, AfterViewChecked {
  @ViewChild('chatContainer') private chatContainer!: ElementRef;
  @ViewChild('messageInput') private messageInput!: ElementRef;
  @Output() closePanelEvent = new EventEmitter<void>();

  messages: ChatMessage[] = [];
  currentMessage: string = '';
  isLoading: boolean = false;
  currentUser: any;
  askedQuestions: Set<string> = new Set(); // Track asked questions
  allQuestions: string[] = []; // Store all available questions
  showRemainingQuestions: boolean = true; // Toggle for showing remaining questions

  constructor(
    private chatbotService: ChatbotService,
    private authService: AuthService
  ) {}

  closePanel(): void {
    this.closePanelEvent.emit();
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.allQuestions = this.getQuickQuestions(); // Store all questions
    this.addWelcomeMessage();
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  addWelcomeMessage(): void {
    const role = this.getUserRole();
    let welcomeText = "Hello! I'm your AI assistant for the Invoice Management System. ";
    
    if (role === 'USER') {
      welcomeText += "I can help you check invoice status, understand workflows, and more. How can I assist you today?";
    } else if (role === 'MANAGER') {
      welcomeText += "I can help you view pending approvals, suggest rejection reasons, and track approval statistics. What would you like to know?";
    } else if (role === 'FINANCE') {
      welcomeText += "I can help you view invoices ready for payment, check payment statistics, and process payment information. How can I help?";
    } else if (role === 'ADMIN') {
      welcomeText += "I can provide system-wide summaries, invoice statistics, payment overviews, and all features available to other roles. What would you like to explore?";
    }

    this.messages.push({
      text: welcomeText,
      isUser: false,
      timestamp: new Date()
    });
  }

  sendMessage(): void {
    if (!this.currentMessage.trim() || this.isLoading) {
      return;
    }

    const userMessage = this.currentMessage.trim();
    
    // Track asked question
    this.askedQuestions.add(userMessage);
    
    this.messages.push({
      text: userMessage,
      isUser: true,
      timestamp: new Date()
    });

    this.currentMessage = '';
    this.isLoading = true;

    this.chatbotService.sendMessage(userMessage).subscribe({
      next: (response: ChatbotResponse) => {
        this.messages.push({
          text: response.response,
          isUser: false,
          timestamp: new Date(),
          suggestion: response.suggestion,
          data: response.data // Include structured data
        });
        this.isLoading = false;
        this.messageInput.nativeElement.focus();
      },
      error: (error) => {
        this.messages.push({
          text: "I'm sorry, I encountered an error. Please try again or contact support.",
          isUser: false,
          timestamp: new Date()
        });
        this.isLoading = false;
        console.error('Chatbot error:', error);
      }
    });
  }

  sendQuickMessage(message: string): void {
    this.currentMessage = message;
    this.sendMessage();
  }

  getRemainingQuestions(): string[] {
    // Return questions that haven't been asked yet
    return this.allQuestions.filter(question => !this.askedQuestions.has(question));
  }

  hasRemainingQuestions(): boolean {
    return this.getRemainingQuestions().length > 0;
  }

  toggleRemainingQuestions(): void {
    this.showRemainingQuestions = !this.showRemainingQuestions;
  }

  getHelp(): void {
    this.isLoading = true;
    this.chatbotService.getHelp().subscribe({
      next: (response: ChatbotResponse) => {
        this.messages.push({
          text: response.response,
          isUser: false,
          timestamp: new Date(),
          suggestion: response.suggestion
        });
        this.isLoading = false;
      },
      error: (error) => {
        this.messages.push({
          text: "I'm sorry, I couldn't load the help information. Please try again.",
          isUser: false,
          timestamp: new Date()
        });
        this.isLoading = false;
        console.error('Help error:', error);
      }
    });
  }

  getUserRole(): string {
    if (this.authService.hasRole('ROLE_ADMIN')) return 'ADMIN';
    if (this.authService.hasRole('ROLE_MANAGER')) return 'MANAGER';
    if (this.authService.hasRole('ROLE_FINANCE')) return 'FINANCE';
    return 'USER';
  }

  getQuickQuestions(): string[] {
    const role = this.getUserRole();
    
    if (role === 'USER') {
      return [
        "What is the status of my invoice?",
        "How do I create a new invoice?",
        "Which invoices are pending approval?",
        "Show me my invoice history",
        "What information do I need to create an invoice?",
        "How do I check if my invoice was approved?",
        "What happens after I submit an invoice?",
        "Can I edit my invoice after submission?",
        "How do I track my invoice status?",
        "What should I do if my invoice is rejected?",
        "How long does invoice approval take?",
        "What are the invoice approval levels?",
        "How do I view my invoice details?",
        "What is the difference between invoice statuses?",
        "How do I add items to my invoice?"
      ];
    } else if (role === 'MANAGER') {
      return [
        "Which invoices are pending for approval?",
        "Show me invoices requiring my approval",
        "What are the approval statistics?",
        "How do I approve an invoice?",
        "How do I reject an invoice?",
        "What should I consider before approving?",
        "Show me high-value invoices pending approval",
        "What are the approval level requirements?",
        "How do I add comments when approving?",
        "Which invoices need immediate attention?",
        "What is the approval workflow process?",
        "How do I track my approval history?",
        "What happens after I approve an invoice?",
        "Can I see who else approved this invoice?",
        "What are common rejection reasons?"
      ];
    } else if (role === 'FINANCE') {
      return [
        "Which invoices are ready for payment?",
        "Show me payment statistics",
        "How many payments were processed today?",
        "What is the total amount paid this month?",
        "How do I record a payment?",
        "Show me overdue invoices",
        "What invoices are partially paid?",
        "How do I process a full payment?",
        "What payment methods are available?",
        "Show me payment history for an invoice",
        "What is the payment workflow?",
        "How do I track payment status?",
        "Which vendors have pending payments?",
        "What is the total outstanding amount?",
        "How do I generate payment reports?"
      ];
    } else {
      return [
        "Show me a summary of total invoices and payments",
        "What are the system statistics?",
        "How many users are in the system?",
        "Show me all pending invoices",
        "What is the total revenue this month?",
        "How many vendors are registered?",
        "Show me system-wide approval statistics",
        "What are the most active users?",
        "How do I manage user roles?",
        "Show me invoice trends and analytics",
        "What is the system health status?",
        "How many invoices were created today?",
        "What is the average invoice amount?",
        "Show me high-risk vendors",
        "What are the system configuration options?"
      ];
    }
  }

  formatMessage(text: string): string {
    // Convert line breaks to HTML
    return text.replace(/\n/g, '<br>');
  }

  hasData(message: ChatMessage): boolean {
    return message.data != null && message.data.length > 0;
  }

  getDataByType(message: ChatMessage, type: string): any[] {
    if (!message.data) return [];
    return message.data.filter(item => item.type === type);
  }

  formatCurrency(amount: string): string {
    if (!amount) return '₹0.00';
    const num = parseFloat(amount);
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(num);
  }

  private scrollToBottom(): void {
    try {
      this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight;
    } catch (err) {
      // Ignore scroll errors
    }
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}

