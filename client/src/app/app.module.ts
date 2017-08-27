import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {HttpModule} from "@angular/http";
import {FormsModule} from "@angular/forms";
import {CustomerService} from "./customer.service";
import {WebSocketService} from "./websocket.service";

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    HttpModule,
    FormsModule,
    BrowserModule
  ],
  providers: [CustomerService, WebSocketService],
  bootstrap: [AppComponent]
})
export class AppModule {
}
