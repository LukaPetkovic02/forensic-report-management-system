import { Component } from "@angular/core";
import { AuthService } from "../../service/auth.service";
import { Router } from '@angular/router';
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { ReportService } from "../../service/report.service";

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [FormsModule, CommonModule],
    templateUrl: './home.component.html'
})
export class HomeComponent{
    constructor(private auth: AuthService, private router: Router, private reportService: ReportService) {}
    
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

    onFileSelected(event: any) {
      const file = event.target.files[0];

      if (file) {
        this.selectedFile = file;

        this.reportService.uploadPdf(file).subscribe({
          next: (dto) => {
            console.log("Received DTO:", dto);

            Object.assign(this.formData, dto);
            this.showForm = true;

            // 🔥 KLJUČNO
            event.target.value = null;
          },
          error: () => {
            alert("Greška prilikom parsiranja PDF-a");
            event.target.value = null;
          }
        });
      }
    }

    confirm() {
      console.log("Confirmed:", this.formData);
      // Ovde će kasnije ići poziv backendu
      alert("Izveštaj potvrđen!");
      this.reset();
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
    }
  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}