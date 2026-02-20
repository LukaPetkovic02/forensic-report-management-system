import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, CanActivate, GuardResult, MaybeAsync, Router, RouterStateSnapshot } from "@angular/router";
import { AuthService } from "../service/auth.service";
import { Observable, map, catchError, of } from "rxjs";

@Injectable({
    providedIn: 'root'
})
export class AuthGuard implements CanActivate{
    constructor(private auth: AuthService, private router: Router){}

    canActivate(): boolean {
        if (this.auth.isLoggedIn()) {
            return true;
        }

        this.router.navigate(['/login']);
        return false;
    }
}