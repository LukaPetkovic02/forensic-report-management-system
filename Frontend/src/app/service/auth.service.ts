import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { tap } from "rxjs/operators";

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private baseUrl = 'http://localhost:8080';
    private loggedIn = false;

    constructor(private http: HttpClient){}

    login(username: string, password: string){
        const body = new URLSearchParams();
        body.set('username', username);
        body.set('password', password);

        return this.http.post(
            `${this.baseUrl}/login`,
            body.toString(),
            {
                headers: { 'Content-Type': 'application/x-www-form-urlencoded'},
                withCredentials: true
            }
        ).pipe(
        tap(() => this.loggedIn = true)
    );
    }

    logout(){
        return this.http.post(`${this.baseUrl}/logout`,{},{
            withCredentials: true
        }).pipe(
            tap(() => this.loggedIn = false)
        );
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