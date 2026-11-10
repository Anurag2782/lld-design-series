package lld.solid.isp;

import lld.common.Check;

interface Treats { String treatPatient(); }
interface Prescribes { String prescribe(String drug); }
interface HandlesBilling { String handleBilling(); }

class Doctor implements Treats, Prescribes {
    public String treatPatient() { return "examined"; }
    public String prescribe(String drug) { return "prescribed " + drug; }
}

/** Receptionist implements ONLY what she does. No forced empty methods. */
class Receptionist implements HandlesBilling {
    public String handleBilling() { return "invoice issued"; }
}

public class Demo {
    public static void main(String[] args) {
        System.out.println("ISP: small role-based interfaces");
        Doctor doctor = new Doctor();
        Receptionist receptionist = new Receptionist();
        Check.that(doctor.treatPatient().equals("examined"), "doctor treats");
        Check.that(doctor.prescribe("paracetamol").equals("prescribed paracetamol"), "doctor prescribes");
        Check.that(receptionist.handleBilling().equals("invoice issued"), "receptionist bills");
        Object r = receptionist;
        Check.that(!(r instanceof Prescribes), "receptionist is not forced to prescribe");
    }
}
