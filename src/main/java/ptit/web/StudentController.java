
package ptit.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ptit.Bill;
import ptit.MonthlyTicket;
import ptit.Room;
import ptit.Service;
import ptit.ServiceStat;
import ptit.Student;
import ptit.StudentService;
import ptit.StudentStat;
import ptit.data.MonthlyTicketRepository;
import ptit.data.MotorbikeRepository;
import ptit.data.RoomRepository;
import ptit.data.ServiceRepository;
import ptit.data.StudentRepository;
import ptit.data.StudentServiceRepository;

@Controller
@RequestMapping("/student")
public class StudentController {
    private final StudentRepository stuRepo;
    private final RoomRepository roomRepo;
    private final MonthlyTicketRepository ticketRepo;
    private final MotorbikeRepository motoRepo;
    private final StudentServiceRepository stsvRepo;
    private final ServiceRepository serviceRepo;

    public StudentController(StudentRepository stuRepo, RoomRepository roomRepo, MonthlyTicketRepository ticketRepo,
            MotorbikeRepository motoRepo, StudentServiceRepository stsvRepo, ServiceRepository serviceRepo) {
        this.stuRepo = stuRepo;
        this.roomRepo = roomRepo;
        this.ticketRepo = ticketRepo;
        this.motoRepo = motoRepo;
        this.stsvRepo = stsvRepo;
        this.serviceRepo = serviceRepo;
    }

    @GetMapping
    public String openQLSV(Model model) {
        model.addAttribute("page", "Quản lý sinh viên");
        return "QLSV.html";
    }

    @GetMapping("/addStudent")
    public String addSV(ServletRequest request, Model model) {
        model.addAttribute("page", "Thêm sinh viên");
        model.addAttribute("add", new Student());
        List<Room> rooms = (List<Room>) roomRepo.findAll();
        model.addAttribute("rooms", rooms);
        model.addAttribute("room", new Room());
        return "addStudent";
    }

    @PostMapping("/addStudent")
    public String addStudentProcess(Student student, ServletRequest request) {
        try {
            String phongid = request.getParameter("phongid");
            Room room = new Room();
            room.setId(Long.parseLong(phongid));
            student.setRoom(room);
            stuRepo.save(student);
        } catch (Exception e) {
            return "redirect:/student/addStudent?error";
        }
        return "redirect:/student/studentFound";
    }

    @GetMapping("/studentFound")
    public String findStudent(ServletRequest request, Model model, String studentName) {

        model.addAttribute("page", "Tìm sinh viên");
        model.addAttribute("student", new Student());
        List<Student> listStudent = (List<Student>) stuRepo.findAll();
        for (Student s : listStudent) {
            Room room = roomRepo.findById(s.getRoom().getId()).get();
            s.setRoom(room);
        }
        model.addAttribute("students", listStudent);
        return "studentFound";
    }

    @PostMapping("/studentFound")
    public String findStudentProcess(String studentName, Model model) {
        try {
            List<Student> listAllStudent = (List<Student>) stuRepo.findAll();
            List<Student> listStudent = new ArrayList<Student>();
            for (Student s : listAllStudent) {
                if (s.getStudentName().contains(studentName)) {
                    listStudent.add(s);
                }
                model.addAttribute("students", listStudent);
                model.addAttribute("student", new Student());
                model.addAttribute("studentName", studentName);
            }
        } catch (Exception e) {
            return "redirect:/studentFound?error";
        }
        return "studentFound";
    }

    @GetMapping("/editStudent")
    public String editStudent(ServletRequest request, ServletResponse response, Student student, Model model) {
        model.addAttribute("page", "Sửa sinh viên");
        Student temp = stuRepo.findById(student.getId()).get();
        List<Room> listRoom = (List<Room>) roomRepo.findAll();
        model.addAttribute("student", temp);
        model.addAttribute("room", new Room());
        model.addAttribute("rooms", listRoom);
        return "editStudent";
    }

    @PostMapping("/editStudent")
    public String editStudentProcess(ServletRequest request, ServletResponse response, Student student) {
        try {
            String phongid = request.getParameter("phongid");
            Room room = roomRepo.findById(Long.parseLong(phongid)).get();
            student.setRoom(room);
            stuRepo.save(student);
        } catch (Exception e) {
            return "redirect:/student/editStudent?error";
        }
        return "redirect:/student/studentFound";
    }

    @GetMapping("/deleteStudent")
    public String deleteStudent(ServletRequest request, Student student) {
        try {
            System.out.println(student.getId());
            ticketRepo.deleteByStudentId(student.getId());
            motoRepo.deleteByStudentId(student.getId());
            stuRepo.delete(student);
        } catch (Exception e) {
            return "redirect:/student/deleteStudent?error";
        }
        return "redirect:/student/studentFound";
    }

    @GetMapping("/bill")
    public String bill(ServletRequest request, Model model) {
        model.addAttribute("page", "Hóa đơn hàng tháng");
        model.addAttribute("bill", new Bill());

        ArrayList<Student> students = (ArrayList<Student>) stuRepo.findAll();
        model.addAttribute("students", students);
        model.addAttribute("hidden", "hidden");
        return "bill";
    }

    @PostMapping("/bill")
    public String postBill(Bill bill, Model model) {
        model.addAttribute("page", "Hóa đơn hàng tháng");

        ArrayList<Student> students = (ArrayList<Student>) stuRepo.findAll();
        model.addAttribute("students", students);
        ArrayList<StudentService> ss = stsvRepo.bill(bill.getStudent().getId(), bill.getMonth());
        ArrayList<MonthlyTicket> mt = ticketRepo.findByStudent(bill.getStudent().getId());
        Float total = (float) 0;
        for (StudentService tempss : ss) {
            total += tempss.getQuantity() * tempss.getService().getPrice();
        }
        total += mt.size() * 100000;
        total += bill.getStudent().getRoom().getPrice();
        bill.setSs(ss);
        bill.setMt(mt);
        bill.setTotal(total);
        model.addAttribute("bill", bill);
        return "bill";
    }

    @GetMapping("/stat")
    public String stat(HttpSession session, Model model) {
        model.addAttribute("page", "Thống kê dịch vụ đã sử dụng");
        model.addAttribute("studentStat", new StudentStat());
        session.removeAttribute("studentStat");

        ArrayList<Student> students = (ArrayList<Student>) stuRepo.findAll();
        model.addAttribute("students", students);
        model.addAttribute("hidden", "hidden");

        return "studentStat";
    }

    @PostMapping("/stat")
    public String postStat(HttpSession session, StudentStat studentStat, Model model) {
        model.addAttribute("page", "Thống kê dịch vụ đã sử dụng của sinh viên");
        model.addAttribute("hidden", null);

        ArrayList<ServiceStat> rs = new ArrayList<>();
        ArrayList<Service> services = (ArrayList<Service>) serviceRepo.findAll();

        for (Service sv : services) {
            ServiceStat temp = new ServiceStat();
            temp.setService(sv);
            temp.setMonth(0);
            ArrayList<StudentService> ss = stsvRepo.stuStat(sv.getId(), studentStat.getStudent().getId());
            temp.setSs(ss);
            Float total = (float) 0;
            int count = 0;
            for (StudentService tempss : ss) {
                total += tempss.getQuantity() * tempss.getService().getPrice();
                count += tempss.getQuantity();
            }
            temp.setTotal(total);
            temp.setCount(count);
            rs.add(temp);
        }
        studentStat.setSs(rs);

        ArrayList<Student> students = (ArrayList<Student>) stuRepo.findAll();
        model.addAttribute("students", students);
        session.setAttribute("studentStat", studentStat);
        return "studentStat";
    }

    @GetMapping("/statDetail")
    public String statDetail(HttpSession session, Model model, @RequestParam(name = "id") Long id) {

        StudentStat ss = (StudentStat) session.getAttribute("studentStat");

        ServiceStat serviceStat = new ServiceStat();
        for (int i = 0; i < ss.getSs().size(); i++) {
            if (id == ss.getSs().get(i).getService().getId()) {
                serviceStat = ss.getSs().get(i);
                break;
            }
        }
        model.addAttribute("page", "Thống kê dịch vụ sử dụng");
        model.addAttribute("ServiceStat", serviceStat);

        return "studentStatDetail";
    }
}
