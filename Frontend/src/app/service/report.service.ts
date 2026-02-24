import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  private apiUrl = 'http://localhost:8080/api/reports';

  constructor(private http: HttpClient) {}

  uploadPdf(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post(`${this.apiUrl}/parse`, formData);
  }

  saveReport(file: File, dto: any): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('dto', new Blob([JSON.stringify(dto)], {type: 'application/json'}));

    return this.http.post(`${this.apiUrl}/upload`, formData);
  }

  searchReports(query: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/search`, {
      params: { query }
    });
  }

  searchBasic(input: string): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/search-basic`,
      { params: { input } }
    );
  }

  searchOrganizationThreat(input: string): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/search-org-threat`,
      { params: { input } }
    );
  }

  searchBehaviorDescription(input: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/search/behavior`, { params: { input } });
  }

  searchKnn(input: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/search/knn`, { params: { input } });
  }
}