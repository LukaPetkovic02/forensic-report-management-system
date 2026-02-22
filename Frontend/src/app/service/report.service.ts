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

  searchReports(expert: string, hash: string, classification: string): Observable<any[]> {
    const params: any = {};
    if (expert) params.expert = expert;
    if (hash) params.hash = hash;
    if (classification) params.classification = classification;

    return this.http.get<any[]>(`${this.apiUrl}/search`, { params });
  }
}