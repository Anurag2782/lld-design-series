import { check } from "../common.ts";

interface Treats { treatPatient(): string }
interface Prescribes { prescribe(drug: string): string }
interface HandlesBilling { handleBilling(): string }

class Doctor implements Treats, Prescribes {
  treatPatient() { return "examined"; }
  prescribe(drug: string) { return `prescribed ${drug}`; }
}

/** Implements ONLY what she does. No forced empty methods. */
class Receptionist implements HandlesBilling {
  handleBilling() { return "invoice issued"; }
}

export function demo(): void {
  console.log("ISP: small role-based interfaces");
  const d = new Doctor(), r = new Receptionist();
  check(d.treatPatient() === "examined", "doctor treats");
  check(d.prescribe("paracetamol") === "prescribed paracetamol", "doctor prescribes");
  check(r.handleBilling() === "invoice issued", "receptionist bills");
  check(!("prescribe" in r), "receptionist is not forced to prescribe");
}
