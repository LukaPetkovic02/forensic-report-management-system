import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/page-response';

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

  searchReports(query: string, page: number, size: number): Observable<PageResponse<any>> {
    return this.http.get<PageResponse<any>>(
      `${this.apiUrl}/search`,
      {params: {query, page, size}}
    )
  }

  searchBasic(input: string, page: number, size: number)
    : Observable<PageResponse<any>> {

    return this.http.get<PageResponse<any>>(
      `${this.apiUrl}/search-basic`,
      { params: { input, page, size } }
    );
  }

  searchOrganizationThreat(input: string, page: number, size: number)
    : Observable<PageResponse<any>> {

    return this.http.get<PageResponse<any>>(
      `${this.apiUrl}/search-org-threat`,
      { params: { input, page, size } }
    );
  }

  searchBehaviorDescription(input: string, page: number, size: number)
    : Observable<PageResponse<any>> {

    return this.http.get<PageResponse<any>>(
      `${this.apiUrl}/search/behavior`,
      { params: { input, page, size } }
    );
  }

  searchKnn(input: string, page: number, size: number)
    : Observable<PageResponse<any>> {

    return this.http.get<PageResponse<any>>(
      `${this.apiUrl}/search/knn`,
      { params: { input, page, size } }
    );
  }

  searchByLocation(location: string, radiusKm: number, page: number, size: number)
    : Observable<PageResponse<any>> {

    return this.http.get<PageResponse<any>>(
      `${this.apiUrl}/search/geo/location`,
      { params: { location, radiusKm, page, size } }
    );
  }
}