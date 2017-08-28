import {Component, OnInit} from "@angular/core";
import "rxjs/add/operator/toPromise";
import {Customer} from "./customer";
import {CustomerService} from "./customer.service";
import {WebSocketService} from "./websocket.service";
import {Observable} from "rxjs/Observable";

@Component({
  selector: 'app-root',
  template: `

    <h1>Hello</h1>

    <li *ngFor="let c of customers">
      <b>{{c.id}} </b>
      {{c.name}}
    </li>

    <li *ngFor="let f of files">
      {{f.sessionId}} {{f.path}}
    </li>

  `
})
export class AppComponent implements OnInit {

  chatUrl = "ws://localhost:8080/websocket/updates";
  customers: Array<Customer>;
  files: Array<FileEvent> = [];

  constructor(private customerService: CustomerService, private ws: WebSocketService) {
  }

  ngOnInit(): void {

    this.customerService
      .customers()
      .subscribe(s => this.customers = s);

    const messages = this.ws.connect(this.chatUrl);

    const updates: Observable<FileEvent> = messages
      .map((response: MessageEvent) => {
        return JSON.parse(response.data);
      });

    updates.subscribe((d: FileEvent) => this.files.push(d));
  }
}

export interface FileEvent {
  path: string;
  sessionId: string;
}
