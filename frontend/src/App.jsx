import { Navigate, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Questions from './pages/Questions.jsx';
import QuestionDetail from './pages/QuestionDetail.jsx';
import Bookmarks from './pages/Bookmarks.jsx';
import RevisionLists from './pages/RevisionLists.jsx';
import Tests from './pages/Tests.jsx';
import TakeTest from './pages/TakeTest.jsx';
import AttemptResult from './pages/AttemptResult.jsx';
import Companies from './pages/Companies.jsx';
import CompanyDetail from './pages/CompanyDetail.jsx';
import AdminDashboard from './pages/admin/AdminDashboard.jsx';
import AdminQuestions from './pages/admin/AdminQuestions.jsx';
import AdminQuestionForm from './pages/admin/AdminQuestionForm.jsx';
import AdminTests from './pages/admin/AdminTests.jsx';
import AdminCompanies from './pages/admin/AdminCompanies.jsx';

// A small helper so every private route is declared in one line.
const priv = (element, adminOnly = false) => (
  <ProtectedRoute adminOnly={adminOnly}>{element}</ProtectedRoute>
);

export default function App() {
  return (
    <>
      <Navbar />
      <main className="container py-4">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route path="/" element={priv(<Dashboard />)} />
          <Route path="/questions" element={priv(<Questions />)} />
          <Route path="/questions/:id" element={priv(<QuestionDetail />)} />
          <Route path="/bookmarks" element={priv(<Bookmarks />)} />
          <Route path="/revision-lists" element={priv(<RevisionLists />)} />
          <Route path="/tests" element={priv(<Tests />)} />
          <Route path="/tests/:testId/take" element={priv(<TakeTest />)} />
          <Route path="/attempts/:attemptId" element={priv(<AttemptResult />)} />
          <Route path="/companies" element={priv(<Companies />)} />
          <Route path="/companies/:id" element={priv(<CompanyDetail />)} />

          <Route path="/admin" element={priv(<AdminDashboard />, true)} />
          <Route path="/admin/questions" element={priv(<AdminQuestions />, true)} />
          <Route path="/admin/questions/new" element={priv(<AdminQuestionForm />, true)} />
          <Route path="/admin/questions/:id/edit" element={priv(<AdminQuestionForm />, true)} />
          <Route path="/admin/tests" element={priv(<AdminTests />, true)} />
          <Route path="/admin/companies" element={priv(<AdminCompanies />, true)} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
