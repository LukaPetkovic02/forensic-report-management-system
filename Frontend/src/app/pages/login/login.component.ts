import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { LoginDetails } from '../../models/login-details.model';
import {MatInputModule} from "@angular/material/input";
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule,
    ReactiveFormsModule,
    MatInputModule,
    MatButtonModule,
    MatFormFieldModule,
    FormsModule,],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  loginForm!: FormGroup;
  errorMessage =  ""
  recoveryMessage = ""
  error = '';

  constructor(private fb: FormBuilder,private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
    });
  }

  onSubmit() {
    var loginDetails: LoginDetails = {
      username: this.loginForm.value.username,
      password: this.loginForm.value.password
    }
    
    this.auth.login(loginDetails).subscribe({
      next: (res) => {
        localStorage.setItem("token", res);
        this.router.navigate(['/home'])
      },
      error: () => this.error = 'Invalid credentials'
    });
  }
}
