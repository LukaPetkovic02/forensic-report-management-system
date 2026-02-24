import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";
import { tap } from "rxjs/operators";
import { LoginDetails } from "../models/login-details.model";

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private baseUrl = 'http://localhost:8080';
    private loggedIn = false;

    private tokenSubject = new BehaviorSubject<string>('');
    public token$ = this.tokenSubject.asObservable();

    constructor(private http: HttpClient){}

    login(loginDetails: LoginDetails): Observable<string> {

        return this.http.post(
            `${this.baseUrl}/login`,
            loginDetails, { responseType: 'text' }
        );
    }

    public getToken(): string {
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined') {
        return localStorage.getItem("token") || '';
    }
    return '';
    }

    public logout() {
        localStorage.removeItem("token");
        this.refreshToken();
    }

    public refreshToken() {
        this.tokenSubject.next(this.getToken())
    }

    checkAuth(){
        return this.http.get(`${this.baseUrl}/api/me`,{
            withCredentials: true
        });
    }

    isLoggedIn() {
        return this.loggedIn;
    }
}