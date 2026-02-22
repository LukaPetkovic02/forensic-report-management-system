import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from "@angular/core";
import { AuthService } from "../../service/auth.service";
import { Router } from '@angular/router';
import { FormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { ReportService } from "../../service/report.service";

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [FormsModule, CommonModule],
    templateUrl: './home.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
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