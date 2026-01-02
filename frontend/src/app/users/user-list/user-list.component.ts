import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService, User } from '../../services/user.service';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit {
  users: User[] = [];
  filteredUsers: User[] = [];
  editingUserId: number | null = null;
  selectedRole: { [key: number]: string } = {};
  availableRoles = ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_USER'];
  errorMessage: string = '';
  
  // Modal
  showConfirmModal: boolean = false;
  confirmTitle: string = 'Confirm Action';
  confirmMessage: string = '';
  confirmCallback: (() => void) | null = null;
  
  // Search
  searchTerm: string = '';
  
  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;
  totalPages: number = 1;

  constructor(
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        this.users = data;
        // Initialize selected role for each user (take first role if multiple exist)
        this.users.forEach(user => {
          this.selectedRole[user.id] = user.roles && user.roles.length > 0 ? user.roles[0] : '';
        });
        this.applyFilters();
      },
      error: (err) => {
        console.error('Error loading users:', err);
        this.errorMessage = 'Failed to load users';
      }
    });
  }

  applyFilters(): void {
    let filtered = this.users;
    
    if (this.searchTerm.trim()) {
      const search = this.searchTerm.toLowerCase().trim();
      filtered = this.users.filter(user => 
        user.username?.toLowerCase().includes(search) ||
        user.email?.toLowerCase().includes(search) ||
        user.roles?.some(role => role.toLowerCase().includes(search)) ||
        (user.isActive ? 'active' : 'inactive').includes(search)
      );
    }
    
    this.filteredUsers = filtered;
    this.totalPages = Math.ceil(this.filteredUsers.length / this.itemsPerPage);
    this.currentPage = 1;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  getPaginatedUsers(): User[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredUsers.slice(startIndex, endIndex);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = 5;
    let startPage = Math.max(1, this.currentPage - Math.floor(maxPages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxPages - 1);
    
    if (endPage - startPage < maxPages - 1) {
      startPage = Math.max(1, endPage - maxPages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }

  Math = Math;

  startEdit(user: User): void {
    this.editingUserId = user.id;
    // Set the first role as selected (user can only have one role)
    this.selectedRole[user.id] = user.roles && user.roles.length > 0 ? user.roles[0] : '';
  }

  cancelEdit(): void {
    this.editingUserId = null;
  }

  selectRole(userId: number, role: string): void {
    this.selectedRole[userId] = role;
  }

  isRoleSelected(userId: number, role: string): boolean {
    return this.selectedRole[userId] === role;
  }

  saveUserRoles(userId: number): void {
    const role = this.selectedRole[userId];
    if (!role) {
      this.errorMessage = 'A role must be selected';
      return;
    }

    this.errorMessage = '';
    this.userService.updateUserRoles(userId, role).subscribe({
      next: () => {
        this.editingUserId = null;
        this.loadUsers();
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Failed to update user role';
        console.error('Error updating user role:', err);
      }
    });
  }

  toggleUserStatus(userId: number): void {
    this.confirmTitle = 'Confirm Status Change';
    this.confirmMessage = 'Are you sure you want to change this user\'s status?';
    this.confirmCallback = () => {
      this.userService.toggleUserStatus(userId).subscribe({
        next: () => {
          this.loadUsers();
        },
        error: (err) => {
          this.errorMessage = err.error?.error || 'Failed to update user status';
          console.error('Error toggling user status:', err);
        }
      });
    };
    this.showConfirmModal = true;
  }

  onConfirm(): void {
    if (this.confirmCallback) {
      this.confirmCallback();
      this.confirmCallback = null;
    }
    this.showConfirmModal = false;
  }

  onCancelConfirm(): void {
    this.showConfirmModal = false;
    this.confirmCallback = null;
  }

  getRoleDisplayName(role: string): string {
    return role.replace('ROLE_', '');
  }
}

