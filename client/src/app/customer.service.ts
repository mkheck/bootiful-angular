import {Observable} from "rxjs/Observable";
import {Customer} from "./customer";
import {Injectable} from "@angular/core";
import {Http} from "@angular/http";

import 'rxjs/add/operator/map';

@Injectable()
export class CustomerService {

  constructor(private http: Http) {
  }

  public customers(): Observable<Customer[]> {
    return this.http
      .get('http://localhost:8080/customers')
      .map(r => r.json() as Customer[]);
  }
}
