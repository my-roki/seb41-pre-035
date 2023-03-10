import { Link } from "react-router-dom";
import "../css/logo.css";

const Logo = ({ text, size }) => {
  // text true시 stack overflow가 보입니다
  // size true시 큰 사이즈, false시 작은 사이즈입니다
  return (
    <Link to="/" className="logoWrapper">
      {size ? <i className="fa-brands fa-stack-overflow logoSize1"></i> : <i className="fa-brands fa-stack-overflow logoSize2"></i>}

      {text ? (
        <div className="logoText">
          <div className="logoText1">stack</div>
          <div className="logoText2">overflow</div>
        </div>
      ) : null}
    </Link>
  );
};

export default Logo;
