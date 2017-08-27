import {Component, OnInit} from '@angular/core';
import 'rxjs/add/operator/toPromise';
import {Customer} from "./customer";
import {CustomerService} from "./customer.service";
import {WebsocketService} from "./websocket.service";
import {Subject} from "rxjs/Subject";

@Component({
  selector: 'app-root',
  template: `

    <h1>Hello</h1>

    <li *ngFor="let c of customers">
      <b>{{c.id}} </b>
      {{c.name}}
    </li>
  `
})
export class AppComponent implements OnInit {

  chatUrl = "ws://localhost:8080/websocket/updates";
  updates: Subject<CustomerUpdatedMessage>;
  customers: Array<Customer>;

  constructor(private customerService: CustomerService, private ws: WebsocketService) {
  }

  ngOnInit(): void {
    this.customerService.customers().subscribe(s => this.customers = s);

    /*this.updates =*/
    /*<Subject<CustomerUpdatedMessage>>*/
    this.ws
      .connect(this.chatUrl)
      .map((response: MessageEvent): CustomerUpdatedMessage => {
        const data = JSON.parse(response.data);
        console.log(JSON.stringify(data));
        return {date: data.date};
      })
      .subscribe();
  }
}


export interface CustomerUpdatedMessage {
  date: Date;
}
