import {Component, OnInit} from "@angular/core";
import "rxjs/add/operator/toPromise";
import {Customer} from "./customer";
import {CustomerService} from "./customer.service";
import {WebsocketService} from "./websocket.service";
import {Observable} from "rxjs/Observable";

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
  customers: Array<Customer>;

  constructor(private customerService: CustomerService, private ws: WebsocketService) {
  }

  ngOnInit(): void {

    this.customerService
      .customers()
      .subscribe(s => this.customers = s);

    const messages = this.ws.connect(this.chatUrl);

    const updates: Observable<CustomerUpdatedMessage> = messages
      .map((response: MessageEvent): CustomerUpdatedMessage => {
        return JSON.parse(response.data);
      });

    updates.subscribe(d => console.log(d));
  }
}


export interface CustomerUpdatedMessage {
  date: Date;
}
