import {Component, OnInit} from '@angular/core';
import 'rxjs/add/operator/toPromise';
import {Customer} from "./customer";
import {CustomerService} from "./customer.service";

@Component({
  selector: 'app-root',
  template: `

    <h1>Hello</h1>

    <li *ngFor="let c of customers   ">
      <b>{{c.id}} </b>
      {{c.name}}
    </li>
  `
})
export class AppComponent implements OnInit {

  customers: Array<Customer>;

  constructor(private customerService: CustomerService) {
  }

  ngOnInit(): void {
    this.customerService.customers().subscribe(s => this.customers = s);
  }
}
