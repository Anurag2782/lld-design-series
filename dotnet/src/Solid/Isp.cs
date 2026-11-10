using Lld.Common;

namespace Lld.Solid.Isp;

interface ITreats { string TreatPatient(); }
interface IPrescribes { string Prescribe(string drug); }
interface IHandlesBilling { string HandleBilling(); }

class Doctor : ITreats, IPrescribes
{
    public string TreatPatient() => "examined";
    public string Prescribe(string drug) => "prescribed " + drug;
}

/// <summary>Receptionist implements ONLY what she does. No forced empty methods.</summary>
class Receptionist : IHandlesBilling
{
    public string HandleBilling() => "invoice issued";
}

public static class Demo
{
    public static void Run()
    {
        Console.WriteLine("ISP: small role-based interfaces");
        var doctor = new Doctor();
        var receptionist = new Receptionist();
        Check.That(doctor.TreatPatient() == "examined", "doctor treats");
        Check.That(doctor.Prescribe("paracetamol") == "prescribed paracetamol", "doctor prescribes");
        Check.That(receptionist.HandleBilling() == "invoice issued", "receptionist bills");
        object r = receptionist;
        Check.That(r is not IPrescribes, "receptionist is not forced to prescribe");
    }
}
