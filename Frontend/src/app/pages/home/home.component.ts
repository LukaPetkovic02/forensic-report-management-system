import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from "@angular/core";
import { AuthService } from "../../service/auth.service";
import { Router } from '@angular/router';
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { ReportService } from "../../service/report.service";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from '@angular/material/icon';

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [FormsModule, CommonModule, MatInputModule, MatButtonModule, MatFormFieldModule, MatIconModule],
    templateUrl: './home.component.html',
    styleUrl: './home.component.css',
    changeDetection: ChangeDetectionStrategy.Default//.OnPush,
})
export class HomeComponent{
    constructor(private auth: AuthService, private router: Router, private reportService: ReportService, private cdr: ChangeDetectorRef) {}
    
    selectedFile: File | null = null;
    showForm = false;

    formData = {
      organizationName: '',
      address: '',
      email: '',
      phone: '',
      fileName: '',
      classification: '',
      hash: '',
      threatName: '',
      behaviorDescription: '',
      forensicExpert1: '',
      forensicExpert2: ''
    };

    activeSearchType: 'basic' | 'boolean' | 'org' | 'behavior' | 'knn' | 'geo' | null = null;

    currentPage = 0;
    pageSize = 10;

    totalPages = 0;
    totalElements = 0;

    searchQuery: string = '';

    searchResults: any[] = [];
    searchPerformed = false;

    basicSearchInput: string = '';

    basicSearch(page: number = 0) {

      this.activeSearchType = 'basic';
      this.currentPage = page;

      this.reportService
      .searchBasic(this.basicSearchInput, this.currentPage, this.pageSize)
      .subscribe({
        next: (response) => {

          this.searchResults = response.content;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;

          this.searchPerformed = true;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error(err);
          alert("Error during basic search!");
        }
      });
    }

    orgThreatInput: string = '';

    searchOrgThreat(page: number = 0) {

      this.activeSearchType = 'org';
      this.currentPage = page;

      this.reportService.searchOrganizationThreat(this.orgThreatInput, this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            this.searchResults = response.content;
            this.totalPages = response.totalPages;
            this.totalElements = response.totalElements;

            this.searchPerformed = true;
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error(err);
            alert("Error during org/threat search!");
          }
        });
    }

    behaviorInput: string = '';

    searchBehavior(page: number = 0) {

      this.activeSearchType = 'behavior';
      this.currentPage = page;

      this.reportService.searchBehaviorDescription(this.behaviorInput, this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            this.searchResults = response.content;
            this.totalPages = response.totalPages;
            this.totalElements = response.totalElements;

            this.searchPerformed = true;
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error(err);
            alert("Error during behavior description search!");
          }
        });
    }

    knnInput: string = '';

    searchKnn(page: number = 0) {

      this.activeSearchType = 'knn';
      this.currentPage = page;
      
      this.reportService.searchKnn(this.knnInput, this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            this.searchResults = response.content;
            this.totalPages = response.totalPages;
            this.totalElements = response.totalElements;

            this.searchPerformed = true;
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error(err);
            alert("Error during KNN search!");
          }
        });
    }

    searchReports(page: number = 0) {
      if(!this.searchQuery.trim()){
        return;
      }

      this.activeSearchType = 'boolean';
      this.currentPage = page;

      this.reportService.searchReports(this.searchQuery, this.currentPage, this.pageSize)
      .subscribe({
        next: (response) => {
          this.searchResults = response.content;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          
          this.searchPerformed = true;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error(err);
          alert("Error during search!");
          this.searchPerformed = true;
        }
      });
    }

    geoLocationInput: string = '';
    geoRadiusKm: number = 5;

    searchGeo(page: number = 0) {

      if (!this.geoLocationInput.trim() || !this.geoRadiusKm) {
        return;
      }

      this.activeSearchType = 'geo';
      this.currentPage = page;

      this.reportService
        .searchByLocation(this.geoLocationInput, this.geoRadiusKm, this.currentPage, this.pageSize)
        .subscribe({
          next: (response) => {
            this.searchResults = response.content;
            this.totalPages = response.totalPages;
            this.totalElements = response.totalElements;

            this.searchPerformed = true;
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error(err);
            alert("Error during geo search!");
            this.searchPerformed = true;
          }
        });
    }

    changePage(page: number) {

    if (page < 0 || page >= this.totalPages) return;

    switch (this.activeSearchType) {
      case 'basic':
        this.basicSearch(page);
        break;

      case 'boolean':
        this.searchReports(page);
        break;

      case 'org':
        this.searchOrgThreat(page);
        break;

      case 'behavior':
        this.searchBehavior(page);
        break;

      case 'knn':
        this.searchKnn(page);
        break;

      case 'geo':
        this.searchGeo(page);
        break;
    }
  }

    onFileSelected(event: any) {
      const file = event.target.files[0];

      if (file) {
        this.selectedFile = file;

        this.reportService.uploadPdf(file).subscribe({
          next: (dto) => {
            console.log("Received DTO:", dto);

            this.formData = { ...dto };
            this.showForm = true;

            this.cdr.markForCheck();

            // event.target.value = null;
          },
          error: () => {
            alert("Greška prilikom parsiranja PDF-a");
            event.target.value = null;
          }
        });
      }
    }

    confirm() {
      if(!this.selectedFile){
        alert("No file selected!");
        return;
      }

      this.reportService.saveReport(this.selectedFile, this.formData)
      .subscribe({
        next: (response) => {
          console.log("Saved report:", response);
          alert("Report saved successfully!");
          this.reset();
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error(err);
          alert("Error saving report!");
        }
      });
    }

    cancel() {
      this.reset();
    }

    private reset(){
      this.selectedFile = null;
      this.showForm = false;
      this.formData = {
        organizationName: '',
        address: '',
        email: '',
        phone: '',
        fileName: '',
        classification: '',
        hash: '',
        threatName: '',
        behaviorDescription: '',
        forensicExpert1: '',
        forensicExpert2: ''
      };
      this.cdr.markForCheck();
    }
  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}